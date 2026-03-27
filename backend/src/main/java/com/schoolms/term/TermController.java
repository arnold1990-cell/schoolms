package com.schoolms.term;

import com.schoolms.academicsession.AcademicSessionRepository;
import com.schoolms.common.ApiResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {
    private final TermRepository repository;
    private final AcademicSessionRepository sessionRepository;

    public record TermRequest(@NotBlank String name, @NotNull Long sessionId, boolean active) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Term>> list() { return ApiResponse.ok("Terms", repository.findAll()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Term> create(@RequestBody TermRequest request) {
        if (request.active()) repository.findByActiveTrue().ifPresent(t -> { t.setActive(false); repository.save(t); });
        Term term = new Term(); term.setName(request.name()); term.setActive(request.active());
        term.setAcademicSession(sessionRepository.findById(request.sessionId()).orElseThrow());
        return ApiResponse.ok("Term created", repository.save(term));
    }
}
