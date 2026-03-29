package com.schoolms.marks;

import com.schoolms.common.ApiResponse;
import com.schoolms.exam.ExamTerm;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
public class MarkController {
    private final MarkService service;

    public record MarkRequest(@NotNull Long examId, @NotNull Long studentId, @NotNull Double score) {}
    public record ClassOption(Long id, String name, String code) {}
    public record SubjectOption(Long id, String name, String code) {}
    public record MarksSetupResponse(
            List<ClassOption> classes,
            List<SubjectOption> subjects,
            List<String> examTypes,
            List<ExamTerm> terms,
            boolean teacherProfileLinked,
            String message
    ) {}
    public record LearnerMarkRow(Long learnerId, String learnerName, Double mark, String grade) {}
    public record LearnerMarkEntry(@NotNull Long learnerId, Double mark) {}
    public record BulkMarkRequest(@NotNull Long classId, @NotNull Long subjectId, @NotNull ExamTerm term, @NotNull String examType, List<LearnerMarkEntry> entries) {}

    @GetMapping("/setup")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<MarksSetupResponse> setupData(Authentication authentication) {
        return ApiResponse.ok("Marks setup data", service.setupData(authentication.getName()));
    }

    @GetMapping("/learners")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<LearnerMarkRow>> learners(@RequestParam Long classId,
                                                       @RequestParam Long subjectId,
                                                       @RequestParam ExamTerm term,
                                                       @RequestParam String examType,
                                                       Authentication authentication) {
        return ApiResponse.ok("Learner marks", service.learnersForSelection(authentication.getName(), classId, subjectId, term, examType));
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<LearnerMarkRow>> saveDraft(@RequestBody BulkMarkRequest request, Authentication authentication) {
        return ApiResponse.ok("Draft marks saved", service.saveDraft(authentication.getName(), request));
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<LearnerMarkRow>> submit(@RequestBody BulkMarkRequest request, Authentication authentication) {
        return ApiResponse.ok("Marks submitted", service.submit(authentication.getName(), request));
    }

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Mark>> listByExam(@PathVariable Long examId) {
        return ApiResponse.ok("Exam marks", service.listByExam(examId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Mark> create(@RequestBody MarkRequest request, Authentication authentication) {
        return ApiResponse.ok("Mark created", service.createSingle(authentication.getName(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Mark> update(@PathVariable Long id, @RequestBody MarkRequest request) {
        return ApiResponse.ok("Mark updated", service.updateSingle(id, request));
    }
}
