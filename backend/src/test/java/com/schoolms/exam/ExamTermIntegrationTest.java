package com.schoolms.exam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.academicsession.AcademicSession;
import com.schoolms.academicsession.AcademicSessionRepository;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamTermIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private SchoolClassRepository classRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    private String adminToken;
    private Long classId;
    private Long subjectId;
    private Long sessionId;

    @BeforeEach
    void setUp() throws Exception {
        examRepository.deleteAll();
        subjectRepository.deleteAll();
        classRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();

        createAdminUser();
        adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Grade 9");
        schoolClass.setCode("G9-A");
        schoolClass = classRepository.save(schoolClass);
        classId = schoolClass.getId();

        Subject subject = new Subject();
        subject.setCode("MTH-09");
        subject.setName("Mathematics");
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setActive(true);
        session = sessionRepository.save(session);
        sessionId = session.getId();
    }

    @Test
    void createExamWithTerm1Succeeds() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_1", "Midterm 1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.term").value("TERM_1"));
    }

    @Test
    void createExamWithTerm2Succeeds() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_2", "Midterm 2")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.term").value("TERM_2"));
    }

    @Test
    void createExamWithTerm3Succeeds() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_3", "Midterm 3")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.term").value("TERM_3"));
    }

    @Test
    void createExamWithoutTermFailsValidation() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"No term exam",
                                  "classId": %d,
                                  "subjectId": %d,
                                  "sessionId": %d,
                                  "examDate":"%s",
                                  "durationMinutes": 90,
                                  "totalMarks": 100,
                                  "status": "DRAFT"
                                }
                                """.formatted(classId, subjectId, sessionId, LocalDate.now().plusDays(7))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void updateExamTermWorks() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_1", "Terminal Assessment")))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long examId = created.path("data").path("id").asLong();

        mockMvc.perform(put("/api/exams/{id}", examId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_3", "Terminal Assessment Updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.term").value("TERM_3"));
    }

    private String createExamPayload(String term, String title) {
        return """
                {
                  "title":"%s",
                  "classId": %d,
                  "subjectId": %d,
                  "term": "%s",
                  "sessionId": %d,
                  "examDate":"%s",
                  "durationMinutes": 90,
                  "totalMarks": 100,
                  "status": "DRAFT"
                }
                """.formatted(title, classId, subjectId, term, sessionId, LocalDate.now().plusDays(10));
    }

    private void createAdminUser() {
        User admin = new User();
        admin.setEmail("admin@schoolms.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
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
