package com.schoolms.subject;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SubjectAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListSubjects() throws Exception {
        Subject subject = new Subject();
        subject.setCode("MTH");
        subject.setName("Mathematics");
        subjectRepository.save(subject);

        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("MTH"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateSubject() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"eng","name":"English"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ENG"))
                .andExpect(jsonPath("$.data.name").value("English"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAssignTeacher() throws Exception {
        Subject subject = new Subject();
        subject.setCode("PHY");
        subject.setName("Physics");
        subject = subjectRepository.save(subject);

        Teacher teacher = createTeacher("teach1@example.com", "T-100");

        mockMvc.perform(put("/api/subjects/{id}/assign-teacher", subject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d}
                                """.formatted(teacher.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignedTeacher.id").value(teacher.getId()));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotCreateSubject() throws Exception {
        mockMvc.perform(post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"bio","name":"Biology"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void teacherCannotAssignTeacherToSubject() throws Exception {
        Subject subject = new Subject();
        subject.setCode("CHE");
        subject.setName("Chemistry");
        subject = subjectRepository.save(subject);

        Teacher teacher = createTeacher("teach2@example.com", "T-101");

        mockMvc.perform(put("/api/subjects/{id}/assign-teacher", subject.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d}
                                """.formatted(teacher.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUserCannotAccessProtectedSubjectEndpoints() throws Exception {
        mockMvc.perform(get("/api/subjects"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/subjects")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"his","name":"History"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/subjects/{id}/assign-teacher", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": 1}
                                """))
                .andExpect(status().isForbidden());
    }

    private Teacher createTeacher(String email, String staffCode) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encoded-password");
        user.setRole(Role.TEACHER);
        user.setEnabled(true);
        user = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setFirstName("Jane");
        teacher.setLastName("Doe");
        teacher.setStaffCode(staffCode);
        teacher.setPhone("000-000-0000");
        teacher.setUser(user);
        return teacherRepository.save(teacher);
    }
}
