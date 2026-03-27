package com.schoolms.result;

import com.schoolms.common.ApiResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/results")
@RequiredArgsConstructor
public class ResultController {
    private final ResultService service;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Map<String, Object>>> student(@PathVariable Long studentId) {
        return ApiResponse.ok("Student results", service.studentResult(studentId));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Map<String, Object>>> clazz(@PathVariable Long classId) {
        return ApiResponse.ok("Class results", service.classResult(classId));
    }
}
