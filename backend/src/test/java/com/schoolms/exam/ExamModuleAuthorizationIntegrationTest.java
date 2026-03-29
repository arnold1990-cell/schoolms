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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExamModuleAuthorizationIntegrationTest {

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
    private String teacherToken;
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

        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        createUser("teacher@schoolms.com", "Teacher123!", Role.TEACHER);
        adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Grade 10");
        schoolClass.setCode("G10-A");
        schoolClass = classRepository.save(schoolClass);
        classId = schoolClass.getId();

        Subject subject = new Subject();
        subject.setCode("ENG-10");
        subject.setName("English");
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        AcademicSession session = new AcademicSession();
        session.setName("2026/2027");
        session.setActive(true);
        session = sessionRepository.save(session);
        sessionId = session.getId();
    }

    @Test
    void authenticatedAdminCanLoadExamPageDataEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/classes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/subjects")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserIsBlockedFromExamPageDataEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/classes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/subjects")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedAdminCanCreateExamWithTerm1() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_1")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedAdminCanUpdateExam() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_1")))
                .andExpect(status().isOk())
                .andReturn();

        long examId = objectMapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/api/exams/{id}", examId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_2")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedTeacherIsBlockedFromExamsEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExamPayload("TERM_1")))
                .andExpect(status().isForbidden());
    }

    private String createExamPayload(String term) {
        return """
                {
                  "title":"English CAT",
                  "classId": %d,
                  "subjectId": %d,
                  "term": "%s",
                  "sessionId": %d,
                  "examDate":"%s",
                  "durationMinutes": 60,
                  "totalMarks": 100,
                  "status": "DRAFT"
                }
                """.formatted(classId, subjectId, term, sessionId, LocalDate.now().plusDays(5));
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
