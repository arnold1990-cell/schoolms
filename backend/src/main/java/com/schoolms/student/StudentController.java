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
    private final StudentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<StudentDtos.StudentResponse>> list(@RequestParam(required = false) String q) {
        return ApiResponse.ok("Students", service.list(q));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDtos.StudentResponse> create(@Valid @RequestBody StudentDtos.StudentRequest request) {
        return ApiResponse.ok("Student created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDtos.StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentDtos.StudentRequest request) {
        return ApiResponse.ok("Student updated", service.update(id, request));
    }
}
