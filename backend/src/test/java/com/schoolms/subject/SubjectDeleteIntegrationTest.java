package com.schoolms.subject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.exam.Exam;
import com.schoolms.exam.ExamRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubjectDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        examRepository.deleteAll();
        subjectRepository.deleteAll();
        userRepository.deleteAll();
        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        createUser("teacher@schoolms.com", "Teacher123!", Role.TEACHER);
    }

    @Test
    void adminCanDeleteSubjectSuccessfully() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        Subject subject = createSubject("BIO101", "Biology");

        mockMvc.perform(delete("/api/subjects/{id}", subject.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Subject deleted successfully"));
    }

    @Test
    void deletingMissingSubjectReturnsNotFound() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(delete("/api/subjects/{id}", 999999L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Subject not found"));
    }

    @Test
    void teacherCannotDeleteSubject() throws Exception {
        String teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");
        Subject subject = createSubject("CHE101", "Chemistry");

        mockMvc.perform(delete("/api/subjects/{id}", subject.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotDeleteSubject() throws Exception {
        Subject subject = createSubject("GEO101", "Geography");

        mockMvc.perform(delete("/api/subjects/{id}", subject.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletingSubjectReferencedByExamReturnsConflict() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        Subject subject = createSubject("MTH101", "Mathematics");

        Exam exam = new Exam();
        exam.setTitle("Math Midterm");
        exam.setSubject(subject);
        examRepository.save(exam);

        mockMvc.perform(delete("/api/subjects/{id}", subject.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete subject because it is referenced by existing exams"));
    }

    private Subject createSubject(String code, String name) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        return subjectRepository.save(subject);
    }

    private void createUser(String email, String password, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setEnabled(true);
        userRepository.save(user);
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
