package com.schoolms.analytics;

import com.schoolms.common.ApiResponse;
import com.schoolms.exam.ExamRepository;
import com.schoolms.notification.NotificationRepository;
import com.schoolms.student.StudentRepository;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.user.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DashboardController {
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/api/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> admin() {
        return ApiResponse.ok("Admin dashboard", Map.of(
                "totalTeachers", teacherRepository.count(),
                "totalStudents", studentRepository.count(),
                "totalClasses", subjectRepository.count(),
                "totalSubjects", subjectRepository.count(),
                "totalExams", examRepository.count()));
    }

    @GetMapping("/api/teacher/dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<Map<String, Object>> teacher(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        return ApiResponse.ok("Teacher dashboard", Map.of(
                "upcomingExams", examRepository.findAll().stream().limit(5).toList(),
                "notifications", notificationRepository.findByRecipientIdOrderByCreatedAtDesc(user.getId())));
    }
}
