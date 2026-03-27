package com.schoolms.academicsession;

import com.schoolms.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class AcademicSessionController {
    private final AcademicSessionRepository repository;

    public record SessionRequest(@NotBlank String name, boolean active) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<AcademicSession>> list() { return ApiResponse.ok("Sessions", repository.findAll()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AcademicSession> create(@RequestBody SessionRequest request) {
        if (request.active()) repository.findByActiveTrue().ifPresent(s -> { s.setActive(false); repository.save(s); });
        AcademicSession session = new AcademicSession(); session.setName(request.name()); session.setActive(request.active());
        return ApiResponse.ok("Session created", repository.save(session));
    }
}
