package org.example.backend.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.backend.config.CustomUserDetails;
import org.example.backend.config.JWt.JwtService;
import org.example.backend.entity.*;
import org.example.backend.enums.AdmissionStatus;
import org.example.backend.exception.ResourceNotFound;
import org.example.backend.mapper.InstructorMapper;
import org.example.backend.mapper.StudentMapper;
import org.example.backend.repository.*;
import org.example.backend.util.AuthResponse;
import org.example.backend.util.JwtResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {
    private final SubmissionReqRepository submissionReqRepository;
    @Value("${application.security.jwt.refresh-token-expiration}")
    private  long REFRESH_VALIDITY ;
    private final AuthenticationManager manager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final FileService fileService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDetailsService userDetailsService;


    public AuthService(AuthenticationManager manager, JwtService jwtService, UserRepository userRepository, StudentRepository studentRepository, InstructorRepository instructorRepository, FileService fileService, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository, SubmissionReqRepository submissionReqRepository, UserDetailsService userDetailsService) {
        this.manager = manager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.instructorRepository = instructorRepository;
        this.fileService = fileService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.submissionReqRepository = submissionReqRepository;
        this.userDetailsService = userDetailsService;
    }

    public boolean isAdmin(UserDetails userDetails)
    {


            return userDetails.getAuthorities().stream()
                    .anyMatch(r -> r.getAuthority().equals("ROLE_ADMIN"));
    }

    public Authentication isAuthenticated(String email, String password)
    {
        try {
            Authentication authentication = manager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            return authentication;
        } catch (BadCredentialsException e) {
            return null; // Return null if credentials are invalid
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed: " + e.getMessage());
//            return null;
        }
    }

    public AuthResponse login(String email, String password,HttpServletResponse servletResponse)
    {
        Optional<SubmissionRequest> request=submissionReqRepository.getByEmail(email);

        AuthResponse response = new AuthResponse();
        if(request.isPresent())
        {
            SubmissionRequest request1= request.get();
            if(!request1.getAdmissionStatus().equals(AdmissionStatus.ACCEPTED))
            {

                response.setEmail(email);
                response.setFirstName(request1.getFirstName());
                response.setLastName(request1.getLastName());
                response.setStatus(request1.getAdmissionStatus().toString());
                return response;
            }
        }


        User user = userRepository.getUserByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalidd email or password."));

        var auth = isAuthenticated(email, password);
        if (auth == null || !auth.isAuthenticated()) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(auth);
        UserDetails userDetails = (UserDetails) auth.getPrincipal();

        // Generate Tokens
        String accessToken = jwtService.generateAccessToken(userDetails);
//        RefreshToken tokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());
//        String refreshToken = tokenEntity.getToken();

        String refreshToken = jwtService.generateRefreshToken(userDetails);
        setRefreshTokenCookie(servletResponse,refreshToken);
        setAccessTokenCookie(servletResponse,accessToken);



        response.setRefreshToken(refreshToken);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setMessage("User login successful");

        // Process User Role: Admin, Student, or Instructor
        if (isAdmin(userDetails)) {
            response.setRoles(getUserRoles(userDetails));
        } else {
            Student student = studentRepository.findByUser(user).orElse(null);
            Instructor instructor = instructorRepository.findByUser(user).orElse(null);

            if (student != null) {
                populateStudentResponse(response, student);
            } else if (instructor != null) {
                populateInstructorResponse(response, instructor);
//                response.setPersonalImage(instructor.);
            } else {
                throw new RuntimeException("User type not recognized");
            }
        }


        return response;
    }

    public AuthResponse checkAuth(String email, HttpServletResponse servletResponse) {
        Optional<SubmissionRequest> request = submissionReqRepository.getByEmail(email);

        AuthResponse response = new AuthResponse();

        // Case 1: User has a pending or rejected submission request
        if (request.isPresent()) {
            SubmissionRequest req = request.get();
            if (!AdmissionStatus.ACCEPTED.equals(req.getAdmissionStatus())) {
                response.setEmail(email);
                response.setFirstName(req.getFirstName());
                response.setLastName(req.getLastName());
                response.setStatus(req.getAdmissionStatus().toString());
                return response;
            }
        }

        // Case 2: Authenticated system user
        User user = userRepository.getUserByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        // Manually create authentication and set it into the security context
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        setAccessTokenCookie(servletResponse, accessToken);
        setRefreshTokenCookie(servletResponse, refreshToken);

        // Build base auth response

        response.setRefreshToken(refreshToken);
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setMessage("User login successful");
        response.setRoles(getUserRoles(userDetails));

        // Add role-specific data
        Student student = studentRepository.findByUser(user).orElse(null);
        Instructor instructor = instructorRepository.findByUser(user).orElse(null);

        if (student != null) {
            populateStudentResponse(response, student);
        } else if (instructor != null) {
            populateInstructorResponse(response, instructor);
        } else if (isAdmin(userDetails)) {
            // Nothing to add, already handled by roles
        } else {
            throw new RuntimeException("User type not recognized");
        }

        return response;
    }



    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookie(request);
        if(refreshToken==null)
        {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing refresh token.");
        }

        String email = jwtService.validateRefreshToken(refreshToken);
        User user = userRepository.getUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFound("User", "email", email));

        UserDetails userDetails =new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);
        setRefreshTokenCookie(response,newRefreshToken);
        setAccessTokenCookie(response,accessToken);
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);

        return ResponseEntity.ok(tokens);
    }

    public JwtResponse getNewAccessToken(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found in database"));
        refreshTokenService.verifyExpiration(storedToken);

        User user = storedToken.getUser();
        if (user == null) {
            throw new IllegalArgumentException("No user associated with refresh token");
        }

        UserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtService.generateAccessToken(userDetails);

        JwtResponse response = new JwtResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);

        return response;
    }

    private List<String> getUserRoles(UserDetails userDetails) {
        return userDetails.getAuthorities().stream()
                .map(role -> role.getAuthority().replace("ROLE_", "")) // Remove "ROLE_" prefix
                .collect(Collectors.toList());
    }

    private void populateStudentResponse(AuthResponse response, Student student) {
        response.setPersonalImage(fileService.getFileName(student.getSubmissionRequest().getPersonalPhoto()));

        response.setRoles(student.getUser().getRoleList().stream()
                .map(role -> role.getRoleName().toString())
                .collect(Collectors.toList()));
    }
    private void populateInstructorResponse(AuthResponse response, Instructor instructor) {
        response.setPersonalImage(fileService.getFileName(instructor.getPersonalImage()));
        response.setRoles(instructor.getUser().getRoleList().stream()
                .map(role -> role.getRoleName().toString())
                .collect(Collectors.toList()));
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals("refreshToken")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private  void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // Secure against XSS
                .secure(true) // Use HTTPS in production
                .sameSite("Strict") // Prevent CSRF attacks
                .path("/auth/refreshToken") // Only used for refresh endpoint
                .maxAge(REFRESH_VALIDITY) // 7 days
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
    }

    public   void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie refreshCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true) // Secure against XSS
                .secure(false) // Use HTTPS in production
                .sameSite("lax") // Prevent CSRF attacks
                .path("/") // Only used for refresh endpoint
                .maxAge(15*60) // 7 days
                .build();

        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
}
