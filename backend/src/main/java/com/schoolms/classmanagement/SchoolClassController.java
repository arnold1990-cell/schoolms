package com.schoolms.classmanagement;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<SchoolClassDtos.SchoolClassSummaryResponse>> list() {
        return ApiResponse.ok("Classes", service.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok("Class", service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.SchoolClassSummaryResponse> create(@Valid @RequestBody SchoolClassDtos.SchoolClassUpsertRequest request) {
        return ApiResponse.ok("Class created", service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.SchoolClassSummaryResponse> update(@PathVariable Long id, @Valid @RequestBody SchoolClassDtos.SchoolClassUpsertRequest request) {
        return ApiResponse.ok("Class updated", service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResponse.ok("Class deleted", null);
    }

    @PutMapping("/{id}/teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> assignTeacher(@PathVariable Long id, @RequestBody SchoolClassDtos.AssignTeacherRequest request) {
        return ApiResponse.ok("Class teacher assignment updated", service.assignTeacher(id, request.teacherId()));
    }

    @PostMapping("/{id}/learners/{learnerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> addLearner(@PathVariable Long id, @PathVariable Long learnerId) {
        return ApiResponse.ok("Learner assigned to class", service.addLearner(id, learnerId));
    }

    @DeleteMapping("/{id}/learners/{learnerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> removeLearner(@PathVariable Long id, @PathVariable Long learnerId) {
        return ApiResponse.ok("Learner removed from class", service.removeLearner(id, learnerId));
    }

    @PutMapping("/{id}/learners/{learnerId}/transfer/{targetClassId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> transferLearner(
            @PathVariable Long id,
            @PathVariable Long learnerId,
            @PathVariable Long targetClassId
    ) {
        return ApiResponse.ok("Learner transferred", service.transferLearner(id, learnerId, targetClassId));
    }

    @PostMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> addSubject(@PathVariable Long id, @PathVariable Long subjectId) {
        return ApiResponse.ok("Subject assigned to class", service.addSubject(id, subjectId));
    }

    @DeleteMapping("/{id}/subjects/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClassDtos.ClassRosterResponse> removeSubject(@PathVariable Long id, @PathVariable Long subjectId) {
        return ApiResponse.ok("Subject removed from class", service.removeSubject(id, subjectId));
    }
}
