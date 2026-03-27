package com.schoolms.grading;

import com.schoolms.common.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {
    private final GradingService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<GradeScale>> list() { return ApiResponse.ok("Grade scale", service.list()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<GradeScale> create(@RequestBody GradeScale scale) { return ApiResponse.ok("Saved", service.save(scale)); }
}
