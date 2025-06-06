package org.example.backend.service;

import org.example.backend.dto.StudentCourseRequestDTO;
import org.example.backend.dto.courseDto.DegreeCourseDTO;
import org.example.backend.entity.*;
import org.example.backend.enums.SemesterName;
import org.example.backend.exception.ResourceNotFound;
import org.example.backend.repository.CourseRepository;
import org.example.backend.repository.SemesterRepository;
import org.example.backend.repository.StudentCourseRepository;
import org.example.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class StudentCoursesService {
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final StudentCourseRepository studentCourseRepository;

    public StudentCoursesService(StudentRepository studentRepository, CourseRepository courseRepository, SemesterRepository semesterRepository, StudentCourseRepository studentCourseRepository) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.semesterRepository = semesterRepository;
        this.studentCourseRepository = studentCourseRepository;
    }

    @Transactional
    public String enrollStudentInCourse(List<StudentCourseRequestDTO> requestDTO, Student student) {
        Semester semester = semesterRepository.findTopSemester()
                .orElseThrow(() -> new RuntimeException("semester not found"));

        SemesterId semesterId = new SemesterId();
        semesterId.setYearLevel(semester.getSemesterId().getYearLevel());
        semesterId.setSemesterName(semester.getSemesterId().getSemesterName());

        for (StudentCourseRequestDTO dto : requestDTO) {
            Optional<StudentCourse> existingEnrollment = studentCourseRepository
                    .findByStudentAndCourseAndSemester(student, dto.getCourseId(), semester);

            if (existingEnrollment.isPresent()) {
                throw new RuntimeException("Student is already enrolled in course ID: " + dto.getCourseId());
            }

            Course course = courseRepository.findById(dto.getCourseId())
                    .orElseThrow(() -> new ResourceNotFound("Course", "id", dto.getCourseId()));

            StudentCourseId id = new StudentCourseId();
            id.setStudentId(student.getStudentId());
            id.setCourseId(dto.getCourseId());
            id.setYearLevel(semesterId.getYearLevel());
            id.setSemesterName(semesterId.getSemesterName().name());

            StudentCourse studentCourse = new StudentCourse();
            studentCourse.setId(id);
            studentCourse.setCourse(course);
            studentCourse.setStudent(student);
            studentCourse.setSemester(semester);

            studentCourseRepository.save(studentCourse);
        }

        return "course signed successfully";
    }


    public List<DegreeCourseDTO> getCoursesWithDegreeByStudentAndSemester(String email,SemesterName semesterName, Integer yearLevel) {
        // Step 1: Find the student by email
        Student student = studentRepository.findStudentByEmail(email)
                .orElseThrow(() -> new ResourceNotFound("Student", "email", email));
        Long studentId = student.getStudentId();

        // Step 2: Retrieve all StudentCourse objects for the student
        List<StudentCourse> studentCourses = studentCourseRepository.findCoursesWithDegreeByStudentAndSemester(studentId,semesterName, yearLevel);

        if (studentCourses.isEmpty()) {
            throw new ResourceNotFound("StudentCourse", "studentId", studentId);
        }

        // Step 3: Group courses by semester
        Map<String, DegreeCourseDTO> semesterMap = new LinkedHashMap<>();

        double cumulativeTotalGradeCredits = 0;
        double cumulativeTotalHours = 0;
        for (StudentCourse sc : studentCourses) {
            String semesterKey = sc.getSemester().getSemesterId().getSemesterName() + "-" + sc.getSemester().getSemesterId().getYearLevel();

            // Create or retrieve the semester DTO
            DegreeCourseDTO semesterDTO = semesterMap.computeIfAbsent(semesterKey, k -> {
                DegreeCourseDTO newSemester = new DegreeCourseDTO();
                newSemester.setSemesterName(sc.getSemester().getSemesterId().getSemesterName());
                newSemester.setSemesterYear(sc.getSemester().getSemesterId().getYearLevel());
                newSemester.setCourses(new ArrayList<>());
                return newSemester;
            });

            // Add the course to the semester
            DegreeCourseDTO.Course course = new DegreeCourseDTO.Course();
            course.setCourseCode(sc.getCourse().getCourseCode());
            course.setCourseName(sc.getCourse().getCourseName());
            course.setHours(sc.getCourse().getCredit());
            course.setDegree(sc.getDegree());
            semesterDTO.getCourses().add(course);

            if (course.getDegree() != null) {

                cumulativeTotalGradeCredits += convertToPoint(course.getDegree()) * course.getHours();
            }
                cumulativeTotalHours += course.getHours();

        }

        System.out.println("all hours "+cumulativeTotalHours );
        System.out.println("all total "+cumulativeTotalGradeCredits );

        double x=0;
        int y=0;
        // Step 4: Calculate GPA for each semester
        for (DegreeCourseDTO semester : semesterMap.values()) {
            x+=calculateGPA(semester);
        }
//        System.out.println(x);
//
//        y= semesterMap.size();
//        System.out.println(y);
//        System.out.println("ccc "+x/y );

        double cumulativeGPA = cumulativeTotalHours > 0 ? cumulativeTotalGradeCredits / cumulativeTotalHours : 0;


        DegreeCourseDTO courseDTO = new DegreeCourseDTO();
        courseDTO.setGpa(cumulativeGPA);
        semesterMap.put("cumulative-gpa",courseDTO);



        // Step 6: Return the list of semesters
        return new ArrayList<>(semesterMap.values());
    }

    private double calculateGPA(DegreeCourseDTO semester) {
        double totalGradeCredits = 0;
        double totalHours = 0;

        for (DegreeCourseDTO.Course course : semester.getCourses()) {
            totalHours += course.getHours();
            if (course.getDegree() != null) {
                double gradePoint = convertToPoint(course.getDegree());
                totalGradeCredits += gradePoint * course.getHours();
            }
        }

        double gpa = totalHours > 0 ? totalGradeCredits / totalHours : 0;
        semester.setGpa(gpa);
        return gpa;
    }

    private static double convertToPoint(double score) {
        if (score >= 90 && score <= 100) {
            return 4.0; // Grade A
        } else if (score >= 70 && score < 90) {
            return 3.0; // Grade B
        } else if (score >= 50 && score < 70) {
            return 2.0; // Grade C
        } else if (score >= 0 && score < 50) {
            return 0.0; // Grade F
        } else {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
    }



}
