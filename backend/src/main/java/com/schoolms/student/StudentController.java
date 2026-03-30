package com.schoolms.student;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> createStudent(@Valid @RequestBody StudentCreateRequest request) {
        return ApiResponse.ok("Student created", studentService.createStudent(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<StudentResponse>> getAllStudents() {
        return ApiResponse.ok("Students", studentService.getAllStudents());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<StudentResponse> getStudentById(@PathVariable Long id) {
        return ApiResponse.ok("Student", studentService.getStudentById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        return ApiResponse.ok("Student updated", studentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}/confirm-delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> confirmDeleteStudent(@PathVariable Long id, @Valid @RequestBody StudentDeleteConfirmationRequest request) {
        studentService.confirmAndDeleteStudent(id, request);
        return ApiResponse.ok("Student deleted", null);
    }
}
