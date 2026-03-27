package com.schoolms.marks;

import com.schoolms.common.ApiResponse;
import com.schoolms.common.AppException;
import com.schoolms.exam.ExamRepository;
import com.schoolms.grading.GradingService;
import com.schoolms.student.StudentRepository;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.user.UserRepository;
import com.schoolms.user.Role;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marks")
@RequiredArgsConstructor
public class MarkController {
    private final MarkRepository repository;
    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final GradingService gradingService;
    private final UserRepository userRepository;

    public record MarkRequest(@NotNull Long examId, @NotNull Long studentId, @NotNull Double score) {}

    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Mark>> listByExam(@PathVariable Long examId) { return ApiResponse.ok("Exam marks", repository.findByExamId(examId)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Mark> create(@RequestBody MarkRequest request, Authentication authentication) {
        repository.findByExamIdAndStudentId(request.examId(), request.studentId()).ifPresent(m -> {
            throw new AppException("Duplicate mark entry", HttpStatus.CONFLICT);
        });
        Mark mark = new Mark();
        var exam = examRepository.findById(request.examId()).orElseThrow();
        if (request.score() < 0 || request.score() > exam.getTotalMarks()) throw new AppException("Score out of range", HttpStatus.BAD_REQUEST);
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        var teacher = teacherRepository.findByUserId(user.getId()).orElse(null);
        if (user.getRole() == Role.TEACHER && teacher == null) {
            throw new AppException("Teacher profile is not linked to this account", HttpStatus.FORBIDDEN);
        }
        if (teacher == null) {
            teacher = exam.getSubject() != null ? exam.getSubject().getAssignedTeacher() : null;
        }
        if (teacher == null) {
            throw new AppException("No teacher is assigned for this mark entry", HttpStatus.BAD_REQUEST);
        }
        mark.setTeacher(teacher);
        mark.setExam(exam);
        mark.setStudent(studentRepository.findById(request.studentId()).orElseThrow());
        mark.setScore(request.score());
        mark.setGrade(gradingService.resolveGrade(request.score()));
        return ApiResponse.ok("Mark created", repository.save(mark));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Mark> update(@PathVariable Long id, @RequestBody MarkRequest request) {
        Mark mark = repository.findById(id).orElseThrow();
        var exam = examRepository.findById(request.examId()).orElseThrow();
        if (request.score() < 0 || request.score() > exam.getTotalMarks()) throw new AppException("Score out of range", HttpStatus.BAD_REQUEST);
        mark.setScore(request.score());
        mark.setGrade(gradingService.resolveGrade(request.score()));
        return ApiResponse.ok("Mark updated", repository.save(mark));
    }
}
