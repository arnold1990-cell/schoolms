package com.schoolms.subject;

import com.schoolms.common.ApiResponse;
import com.schoolms.teacher.TeacherRepository;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
public class SubjectController {
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    public record SubjectRequest(@NotBlank String code, @NotBlank String name, Long teacherId) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Subject>> list() { return ApiResponse.ok("Subjects", subjectRepository.findAll()); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Subject> create(@RequestBody SubjectRequest request) {
        Subject s = new Subject(); s.setCode(request.code()); s.setName(request.name());
        if (request.teacherId() != null) teacherRepository.findById(request.teacherId()).ifPresent(s::setAssignedTeacher);
        return ApiResponse.ok("Subject created", subjectRepository.save(s));
    }
}
