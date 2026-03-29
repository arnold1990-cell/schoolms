package com.schoolms.exam;

import com.schoolms.academicsession.AcademicSessionRepository;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.common.ApiResponse;
import com.schoolms.subject.SubjectRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {
    private final ExamRepository examRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicSessionRepository sessionRepository;

    public record ExamRequest(@NotBlank String title, @NotNull Long classId, @NotNull Long subjectId, @NotNull ExamTerm term,
                              @NotNull Long sessionId, @NotNull LocalDate examDate, @NotNull Integer durationMinutes,
                              @NotNull Double totalMarks, ExamStatus status) {}

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<Exam>> list(@RequestParam(required = false) Long classId,
                                       @RequestParam(required = false) Long subjectId,
                                       @RequestParam(required = false) ExamStatus status) {
        List<Exam> exams = examRepository.findAll().stream()
                .filter(exam -> classId == null || (exam.getSchoolClass() != null && classId.equals(exam.getSchoolClass().getId())))
                .filter(exam -> subjectId == null || (exam.getSubject() != null && subjectId.equals(exam.getSubject().getId())))
                .filter(exam -> status == null || status == exam.getStatus())
                .toList();
        return ApiResponse.ok("Exams", exams);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> create(@Valid @RequestBody ExamRequest request) {
        Exam e = new Exam();
        e.setTitle(request.title());
        e.setSchoolClass(classRepository.findById(request.classId()).orElseThrow());
        e.setSubject(subjectRepository.findById(request.subjectId()).orElseThrow());
        e.setTerm(request.term());
        e.setAcademicSession(sessionRepository.findById(request.sessionId()).orElseThrow());
        e.setExamDate(request.examDate()); e.setDurationMinutes(request.durationMinutes());
        e.setTotalMarks(request.totalMarks()); e.setStatus(request.status() == null ? ExamStatus.DRAFT : request.status());
        e.setExamCode("EXM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        return ApiResponse.ok("Exam created", examRepository.save(e));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Exam> update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        Exam e = examRepository.findById(id).orElseThrow();
        e.setTitle(request.title());
        e.setSchoolClass(classRepository.findById(request.classId()).orElseThrow());
        e.setSubject(subjectRepository.findById(request.subjectId()).orElseThrow());
        e.setTerm(request.term());
        e.setAcademicSession(sessionRepository.findById(request.sessionId()).orElseThrow());
        e.setExamDate(request.examDate());
        e.setDurationMinutes(request.durationMinutes());
        e.setTotalMarks(request.totalMarks());
        e.setStatus(request.status() == null ? ExamStatus.DRAFT : request.status());
        return ApiResponse.ok("Exam updated", examRepository.save(e));
    }
}
