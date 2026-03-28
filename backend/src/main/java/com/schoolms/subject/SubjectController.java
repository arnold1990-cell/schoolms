package com.schoolms.subject;

import com.schoolms.common.ApiResponse;
import com.schoolms.common.AppException;
import com.schoolms.teacher.TeacherRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public record SubjectRequest(@NotBlank String code, @NotBlank String name) {}
    public record AssignTeacherRequest(Long teacherId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Subject>> list() {
        return ApiResponse.ok("Subjects", subjectRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> create(@Valid @RequestBody SubjectRequest request) {
        Subject subject = new Subject();
        subject.setCode(request.code().trim().toUpperCase());
        subject.setName(request.name().trim());
        return ApiResponse.ok("Subject created", subjectRepository.save(subject));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> update(@PathVariable Long id, @Valid @RequestBody SubjectRequest request) {
        Subject subject = getSubject(id);
        subject.setCode(request.code().trim().toUpperCase());
        subject.setName(request.name().trim());
        return ApiResponse.ok("Subject updated", subjectRepository.save(subject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Subject subject = getSubject(id);
        subjectRepository.delete(subject);
        return ApiResponse.ok("Subject deleted", null);
    }

    @PutMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> assignTeacher(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return assignTeacherInternal(id, request);
    }

    @PostMapping("/{id}/assign-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> assignTeacherPost(@PathVariable Long id, @RequestBody AssignTeacherRequest request) {
        return assignTeacherInternal(id, request);
    }

    private ApiResponse<Subject> assignTeacherInternal(Long id, AssignTeacherRequest request) {
        Subject subject = getSubject(id);
        if (request.teacherId() == null) {
            subject.setAssignedTeacher(null);
        } else {
            subject.setAssignedTeacher(teacherRepository.findById(request.teacherId())
                    .orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND)));
        }
        return ApiResponse.ok("Subject teacher assignment updated", subjectRepository.save(subject));
    }

    private Subject getSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new AppException("Subject not found", HttpStatus.NOT_FOUND));
    }
}
