package com.schoolms.marks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.exam.ExamTerm;
import com.schoolms.grading.GradeScale;
import com.schoolms.grading.GradeScaleRepository;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.student.StudentStatus;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarksEntryFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PasswordEncoder passwordEncoder;

    @Autowired private UserRepository userRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SchoolClassRepository classRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private GradeScaleRepository gradeScaleRepository;

    private SchoolClass schoolClass;
    private Subject subject;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
        subjectRepository.deleteAll();
        classRepository.deleteAll();
        teacherRepository.deleteAll();
        gradeScaleRepository.deleteAll();
        userRepository.deleteAll();

        User admin = createUser("admin@schoolms.com", Role.ADMIN);
        User teacherUser = createUser("teacher@schoolms.com", Role.TEACHER);
        createTeacher(teacherUser);
        createTeacher(admin);

        schoolClass = new SchoolClass();
        schoolClass.setName("Grade 7 A");
        schoolClass.setCode("G7A");
        schoolClass = classRepository.save(schoolClass);

        subject = new Subject();
        subject.setCode("MATH101");
        subject.setName("Mathematics");
        subject.setAssignedTeacher(teacherRepository.findByUserId(teacherUser.getId()).orElseThrow());
        subject = subjectRepository.save(subject);
        schoolClass.getSubjects().add(subject);
        classRepository.save(schoolClass);

        createStudent("ADM-001", "Jane", "Doe", schoolClass);
        createStudent("ADM-002", "John", "Smith", schoolClass);

        GradeScale scale = new GradeScale();
        scale.setGrade("A");
        scale.setMinScore(80d);
        scale.setMaxScore(100d);
        gradeScaleRepository.save(scale);
    }

    @Test
    void teacherCanLoadMarksSetupDataAndLearners() throws Exception {
        String token = loginAndGetToken("teacher@schoolms.com", "Password123!");

        mockMvc.perform(get("/api/marks/setup").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classes[0].id").isNumber())
                .andExpect(jsonPath("$.data.subjects[0].id").isNumber())
                .andExpect(jsonPath("$.data.examTypes[0]").value("TEST"));

        mockMvc.perform(get("/api/marks/learners")
                        .header("Authorization", "Bearer " + token)
                        .param("classId", schoolClass.getId().toString())
                        .param("subjectId", subject.getId().toString())
                        .param("term", ExamTerm.TERM_1.name())
                        .param("examType", "TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].learnerId").isNumber())
                .andExpect(jsonPath("$.data[0].learnerName").isString());
    }

    @Test
    void teacherCanSaveDraftAndSubmitMarks() throws Exception {
        String token = loginAndGetToken("teacher@schoolms.com", "Password123!");
        Long learnerId = studentRepository.findAll().get(0).getId();

        String payload = """
                {
                  "classId": %d,
                  "subjectId": %d,
                  "term": "TERM_1",
                  "examType": "TEST",
                  "entries": [{"learnerId": %d, "mark": 85}]
                }
                """.formatted(schoolClass.getId(), subject.getId(), learnerId);

        mockMvc.perform(post("/api/marks/draft")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].grade").value("A"));

        mockMvc.perform(post("/api/marks/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].grade").value("A"));
    }

    @Test
    void marksSubmissionFailsWhenRequiredSelectionsMissing() throws Exception {
        String token = loginAndGetToken("teacher@schoolms.com", "Password123!");

        mockMvc.perform(post("/api/marks/submit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "subjectId": %d,
                                  "term": "TERM_1",
                                  "examType": "TEST",
                                  "entries": []
                                }
                                """.formatted(subject.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthorizedUserBlockedFromMarksEntryFlow() throws Exception {
        mockMvc.perform(get("/api/marks/setup")).andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/marks/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123!"));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private void createTeacher(User user) {
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacher.setFirstName("Teacher");
        teacher.setLastName(user.getRole().name());
        teacher.setEmployeeNumber("EMP-" + user.getId());
        teacher.setEmail(user.getEmail());
        teacherRepository.save(teacher);
    }

    private void createStudent(String admission, String firstName, String lastName, SchoolClass assignedClass) {
        Student student = new Student();
        student.setAdmissionNumber(admission);
        student.setFirstName(firstName);
        student.setLastName(lastName);
        student.setGender("FEMALE");
        student.setDateOfBirth(LocalDate.of(2012, 1, 1));
        student.setGrade("Grade 7");
        student.setEnrollmentDate(LocalDate.of(2024, 1, 10));
        student.setGuardianName("Parent");
        student.setGuardianRelationship("PARENT");
        student.setGuardianPhone("1234567");
        student.setAddress("Address");
        student.setStatus(StudentStatus.ACTIVE);
        student.setUsesTransport(false);
        student.setSchoolClass(assignedClass);
        studentRepository.save(student);
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
