package com.schoolms.subject;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    public record AssignTeacherRequest(Long teacherId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<SubjectResponse>> list() {
        return ApiResponse.ok("Subjects", subjectService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("Subject created", subjectService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("Subject updated", subjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.ok("Subject deleted", null);
    }

    @PutMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> assignTeacher(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return ApiResponse.ok("Subject teacher assignment updated", subjectService.assignTeacher(id, request.teacherId()));
    }

    @PostMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SubjectResponse> assignTeacherPost(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return ApiResponse.ok("Subject teacher assignment updated", subjectService.assignTeacher(id, request.teacherId()));
    }
}
