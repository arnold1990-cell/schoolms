package com.schoolms.teacher;

import com.schoolms.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TeacherDtos.TeacherResponse>> list() {
        return ApiResponse.ok("Teachers", teacherService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> get(@PathVariable Long id) {
        return ApiResponse.ok("Teacher", teacherService.get(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TeacherDtos.TeacherResponse>> search(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.ok("Teachers", teacherService.search(keyword));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> create(@Valid @RequestBody TeacherDtos.UpsertTeacherRequest request) {
        return ApiResponse.ok("Teacher created", teacherService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> update(@PathVariable Long id, @Valid @RequestBody TeacherDtos.UpsertTeacherRequest request) {
        return ApiResponse.ok("Teacher updated", teacherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        teacherService.deleteOrDeactivate(id);
        return ApiResponse.ok("Teacher deactivated", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> status(@PathVariable Long id, @RequestParam boolean enabled) {
        return ApiResponse.ok("Teacher status updated", teacherService.setStatus(id, enabled));
    }
}
