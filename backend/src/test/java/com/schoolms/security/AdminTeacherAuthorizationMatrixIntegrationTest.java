package com.schoolms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminTeacherAuthorizationMatrixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();

        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        createUser("teacher@schoolms.com", "Teacher123!", Role.TEACHER);
    }

    @Test
    void adminCanGetSubjectList() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(get("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanCreateSubject() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MTH","name":"Mathematics"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotCreateSubject() throws Exception {
        String teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");

        mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MTH","name":"Mathematics"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAssignTeacherToSubject() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        Subject subject = createSubject(adminToken);
        Teacher teacher = createTeacher("assigned.teacher@schoolms.com", "T-301");

        mockMvc.perform(post("/api/subjects/{id}/assign-teacher", subject.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d}
                                """.formatted(teacher.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotAssignTeacherToSubject() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        String teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");
        Subject subject = createSubject(adminToken);
        Teacher teacher = createTeacher("assigned.teacher@schoolms.com", "T-302");

        mockMvc.perform(post("/api/subjects/{id}/assign-teacher", subject.getId())
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"teacherId": %d}
                                """.formatted(teacher.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessExamsEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanAccessResultsEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(get("/api/results/class/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUsersReceive401OnProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/subjects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"MTH","name":"Mathematics"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void createUser(String email, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
    }

    private Subject createSubject(String adminToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"PHY","name":"Physics"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        long subjectId = payload.path("data").path("id").asLong();
        return subjectRepository.findById(subjectId).orElseThrow();
    }

    private Teacher createTeacher(String email, String staffCode) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Teacher123!"));
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

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("accessToken").asText();
    }
}
