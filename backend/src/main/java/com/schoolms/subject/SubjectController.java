package com.schoolms.subject;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectService subjectService;

    public record SubjectRequest(@NotBlank String code, @NotBlank String name) {}
    public record AssignTeacherRequest(Long teacherId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Subject>> list() {
        return ApiResponse.ok("Subjects", subjectService.list());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> create(@Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("Subject created", subjectService.create(request.code(), request.name()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        return ApiResponse.ok("Subject updated", subjectService.update(id, request.code(), request.name()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.ok("Subject deleted", null);
    }

    @PutMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> assignTeacher(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return assignTeacherInternal(id, request);
    }

    @PostMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> assignTeacherPost(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return assignTeacherInternal(id, request);
    }

    private ApiResponse<Subject> assignTeacherInternal(Long id, AssignTeacherRequest request) {
        return ApiResponse.ok("Subject teacher assignment updated", subjectService.assignTeacher(id, request.teacherId()));
    }
}
