package com.schoolms.teacher;

import com.schoolms.common.ApiResponse;
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
    public ApiResponse<List<TeacherDtos.TeacherResponse>> list() { return ApiResponse.ok("Teachers", teacherService.list()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> create(@RequestBody TeacherDtos.CreateTeacherRequest request) {
        return ApiResponse.ok("Teacher created", teacherService.create(request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TeacherDtos.TeacherResponse> status(@PathVariable Long id, @RequestParam boolean enabled) {
        return ApiResponse.ok("Teacher status updated", teacherService.setStatus(id, enabled));
    }
}
