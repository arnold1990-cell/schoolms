package com.schoolms.student;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@Slf4j
@RequiredArgsConstructor
public class StudentController {
    private final StudentService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<StudentDtos.StudentResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) StudentStatus status
    ) {
        String effectiveKeyword = (keyword == null || keyword.isBlank()) ? q : keyword;
        return ApiResponse.ok("Students", service.list(effectiveKeyword, grade, classId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<StudentDtos.StudentResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Student", service.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<StudentDtos.StudentResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) StudentStatus status
    ) {
        String effectiveKeyword = (keyword == null || keyword.isBlank()) ? q : keyword;
        return ApiResponse.ok("Students", service.list(effectiveKeyword, grade, classId, status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDtos.StudentResponse> create(@Valid @RequestBody StudentDtos.StudentRequest request) {
        log.debug("Incoming learner create payload: admissionNumber={}, firstName={}, lastName={}, classId={}, status={}",
                request.admissionNumber(), request.firstName(), request.lastName(), request.classId(), request.status());
        return ApiResponse.ok("Student created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentDtos.StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentDtos.StudentRequest request) {
        return ApiResponse.ok("Student updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Student deleted", null);
    }
}
