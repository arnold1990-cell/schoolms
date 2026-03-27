package com.schoolms.analytics;

import com.schoolms.exam.ExamRepository;
import com.schoolms.marks.MarkRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.TeacherRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final MarkRepository markRepository;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public Map<String, Object> overview() {
        long pass = markRepository.findAll().stream().filter(m -> m.getGrade() != null && !m.getGrade().equalsIgnoreCase("F")).count();
        long fail = Math.max(0, markRepository.count() - pass);
        double avg = markRepository.findAll().stream().mapToDouble(m -> m.getScore() == null ? 0 : m.getScore()).average().orElse(0);
        return Map.of(
                "totalTeachers", teacherRepository.count(),
                "totalStudents", studentRepository.count(),
                "totalSubjects", subjectRepository.count(),
                "totalExams", examRepository.count(),
                "passCount", pass,
                "failCount", fail,
                "averageScore", avg
        );
    }
}
