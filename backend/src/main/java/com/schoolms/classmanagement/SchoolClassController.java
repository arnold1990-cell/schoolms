package com.schoolms.classmanagement;

import com.schoolms.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class SchoolClassController {
    private final SchoolClassRepository repository;

    public record ClassRequest(@NotBlank String name, String stream) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<SchoolClass>> list() { return ApiResponse.ok("Classes", repository.findAll()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<SchoolClass> create(@RequestBody ClassRequest request) {
        SchoolClass c = new SchoolClass(); c.setName(request.name()); c.setStream(request.stream());
        return ApiResponse.ok("Class created", repository.save(c));
    }
}
