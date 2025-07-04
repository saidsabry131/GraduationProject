package org.example.backend.controller;

import org.example.backend.dto.taskDTO.ResponseTaskDTO;
import org.example.backend.dto.taskDTO.ResponseTaskSubmissionDTO;
import org.example.backend.dto.taskDTO.TaskSubmissionDTO;
import org.example.backend.service.TaskService;
import org.example.backend.service.TaskSubmissionService;
import org.example.backend.util.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/task/student")
public class TaskStudentController {

    private final TaskService taskService;
    private final TaskSubmissionService taskSubmissionService;

    public TaskStudentController(TaskService taskService, TaskSubmissionService taskSubmissionService) {
        this.taskService = taskService;
        this.taskSubmissionService = taskSubmissionService;
    }

    @PostMapping("/submit/{taskId}")
    public ResponseEntity<String> submitTask(@PathVariable int taskId, @ModelAttribute TaskSubmissionDTO taskSubmissionDTO) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email =authentication.getName();

        taskSubmissionService.submitTask(taskId,email,taskSubmissionDTO);
        return ResponseEntity.ok("Task submitted successfully");
    }


    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<ResponseTaskDTO>> getCourseTask(@PathVariable int courseId) {


        return ResponseEntity.ok(taskService.getAllTasksByCourseId(courseId));
    }

    @GetMapping("/upcoming-deadline")
    public ResponseEntity<List<ResponseTaskDTO>> getUpcomingDeadlines() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        List<ResponseTaskDTO> tasks = taskService.getUpcomingDeadlines(email);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/past-deadline")
    public ResponseEntity<List<ResponseTaskDTO>> getPastDeadlines() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        List<ResponseTaskDTO> tasks = taskService.getPastDeadlines(email);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/completed")
    public ResponseEntity<List<ResponseTaskDTO>> getCompletedTasks() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        List<ResponseTaskDTO> tasks = taskService.getCompletedTasks(email);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/submitted")
    public ResponseEntity<List<ResponseTaskSubmissionDTO>> getSubmittedTasks() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        List<ResponseTaskSubmissionDTO> tasks = taskSubmissionService.getAllTaskSubmittedByStudent(email);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/course/{courseId}/upcoming-deadline")
    public ResponseEntity<List<ResponseTaskDTO>> getCourseTasks(
            @PathVariable int courseId,
            @CurrentUser String email
    ) {
       return ResponseEntity.ok( taskService.getUpcomingDeadlines(email, (long) courseId));



    }

    @GetMapping("/course/{courseId}/past-deadline")
    public ResponseEntity<List<ResponseTaskDTO>> getCourseTasksForPastDeadline(
            @PathVariable int courseId,
            @CurrentUser String email
    ) {
        return ResponseEntity.ok( taskService.getPastDeadlines(email, (long) courseId));



    }

    @GetMapping("/course/{courseId}/complete")
    public ResponseEntity<List<ResponseTaskDTO>> getCourseTasksForCompleted(
            @PathVariable int courseId,
            @CurrentUser String email
    ) {
        return ResponseEntity.ok( taskService.getCompletedTasks(email, (long) courseId));



    }

    @GetMapping("/course/{courseId}/task/{taskId}")
    public ResponseEntity<ResponseTaskDTO> getTaskById(@PathVariable int courseId, @PathVariable int taskId )
    {
        ResponseTaskDTO task = taskService.getTaskById(taskId,courseId);
        return ResponseEntity.ok(task);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ResponseTaskDTO> getTaskById(@PathVariable int taskId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        ResponseTaskDTO task = taskService.getTaskById(taskId);
        return ResponseEntity.ok(task);
    }

}
