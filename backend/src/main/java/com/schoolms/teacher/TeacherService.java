package com.schoolms.teacher;

import com.schoolms.common.AppException;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TeacherDtos.TeacherResponse> list() {
        return teacherRepository.findAll().stream().map(this::map).toList();
    }

    public TeacherDtos.TeacherResponse create(TeacherDtos.CreateTeacherRequest request) {
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.TEACHER);
        user.setEnabled(true);
        User savedUser = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setFirstName(request.firstName());
        teacher.setLastName(request.lastName());
        teacher.setStaffCode(request.staffCode());
        teacher.setPhone(request.phone());
        teacher.setUser(savedUser);
        return map(teacherRepository.save(teacher));
    }

    public TeacherDtos.TeacherResponse setStatus(Long id, boolean enabled) {
        Teacher teacher = teacherRepository.findById(id).orElseThrow(() -> new AppException("Teacher not found", HttpStatus.NOT_FOUND));
        teacher.getUser().setEnabled(enabled);
        userRepository.save(teacher.getUser());
        return map(teacher);
    }

    private TeacherDtos.TeacherResponse map(Teacher teacher) {
        return new TeacherDtos.TeacherResponse(teacher.getId(), teacher.getFirstName(), teacher.getLastName(), teacher.getStaffCode(),
                teacher.getPhone(), teacher.getUser().getEmail(), teacher.getUser().isEnabled());
    }
}
