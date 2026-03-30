package com.schoolms.classmanagement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.student.Student;
import com.schoolms.student.StudentRepository;
import com.schoolms.student.StudentStatus;
import com.schoolms.subject.Subject;
import com.schoolms.subject.SubjectRepository;
import com.schoolms.teacher.EmploymentType;
import com.schoolms.teacher.Teacher;
import com.schoolms.teacher.TeacherGender;
import com.schoolms.teacher.TeacherRepository;
import com.schoolms.teacher.TeacherStatus;
import com.schoolms.teacher.TeacherTitle;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SchoolClassIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SchoolClassRepository classRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SubjectRepository subjectRepository;

    private String adminToken;
    private String teacherToken;
    private Long classAId;
    private Long classBId;
    private Long teacherId;
    private Long learnerId;
    private Long subjectId;

    @BeforeEach
    void setUp() throws Exception {
        studentRepository.deleteAll();
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
        classRepository.deleteAll();
        userRepository.deleteAll();

        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        createUser("teacher@schoolms.com", "Teacher123!", Role.TEACHER);
        adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
        teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");

        classAId = createClass("8", "A", "2026", 40);
        classBId = createClass("9", "A", "2026", 45);
        teacherId = createTeacher("home.teacher@schoolms.com", "T-900");
        learnerId = createLearner("ADM-900", classAId);
        subjectId = createSubject("SCI900", "Science");
    }

    @Test
    void adminCanCreateUpdateAndDeleteClass() throws Exception {
        Long classId = createClass("10", "A", "2026", 30);

        mockMvc.perform(put("/api/classes/{id}", classId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gradeLevel":"10","streamSection":"b","academicYear":"2027","capacity":35,"status":"INACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Grade 10B"))
                .andExpect(jsonPath("$.data.stream").value("B"))
                .andExpect(jsonPath("$.data.academicYear").value("2027"));

        mockMvc.perform(delete("/api/classes/{id}", classId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void preventsDuplicateClassByGradeStreamAndYear() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gradeLevel":"8","streamSection":"A","academicYear":"2026","status":"ACTIVE"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Class already exists for Grade 8, Stream A, Academic Year 2026"));
    }

    @Test
    void createClassNormalizesStreamAndGeneratesNameAndAllowsOptionalCapacity() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gradeLevel":" 1 ","streamSection":" a ","academicYear":"2028","status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Grade 1A"))
                .andExpect(jsonPath("$.data.stream").value("A"))
                .andExpect(jsonPath("$.data.capacity").isEmpty());
    }

    @Test
    void rejectsInvalidOrMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gradeLevel":"","streamSection":"","academicYear":"20A6","capacity":0,"status":"ACTIVE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanAssignAndRemoveTeacher() throws Exception {
        mockMvc.perform(put("/api/classes/{id}/teacher", classAId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"teacherId\":" + teacherId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classTeacher.id").value(teacherId));

        mockMvc.perform(put("/api/classes/{id}/teacher", classAId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classTeacher").isEmpty());
    }

    @Test
    void adminCanAddTransferAndRemoveLearner() throws Exception {
        Long newLearnerId = createLearner("ADM-901", classBId);

        mockMvc.perform(post("/api/classes/{id}/learners/{learnerId}", classAId, newLearnerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/classes/{id}/learners/{learnerId}/transfer/{targetClassId}", classAId, learnerId, classBId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(classBId));

        mockMvc.perform(delete("/api/classes/{id}/learners/{learnerId}", classBId, learnerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateAndInvalidLearnerOperationsAreRejected() throws Exception {
        mockMvc.perform(post("/api/classes/{id}/learners/{learnerId}", classAId, learnerId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/classes/{id}/learners/{learnerId}/transfer/{targetClassId}", classAId, learnerId, classAId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/classes/{id}/learners/{learnerId}", classAId, 999999)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanAssignAndRemoveSubjectAndRejectDuplicates() throws Exception {
        mockMvc.perform(post("/api/classes/{id}/subjects/{subjectId}", classAId, subjectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/classes/{id}/subjects/{subjectId}", classAId, subjectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/classes/{id}/subjects/{subjectId}", classAId, subjectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void nonAdminCannotPerformProtectedClassActions() throws Exception {
        mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"gradeLevel\":\"11\",\"streamSection\":\"A\",\"academicYear\":\"2026\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/classes/{id}/teacher", classAId)
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"teacherId\":" + teacherId + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteFailsSafelyWhenDependenciesExist() throws Exception {
        mockMvc.perform(delete("/api/classes/{id}", classAId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete class because learners are still assigned"));
    }

    private Long createClass(String level, String stream, String year, Integer capacity) throws Exception {
        String capacityJson = capacity == null ? "null" : capacity.toString();
        MvcResult result = mockMvc.perform(post("/api/classes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gradeLevel":"%s","streamSection":"%s","academicYear":"%s","capacity":%s,"status":"ACTIVE"}
                                """.formatted(level, stream, year, capacityJson)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("id").asLong();
    }

    private Long createTeacher(String email, String staffCode) {
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
        teacher.setTitle(TeacherTitle.MS);
        teacher.setGender(TeacherGender.FEMALE);
        teacher.setDepartment("Academics");
        teacher.setSpecialization("Science");
        teacher.setEmploymentType(EmploymentType.FULL_TIME);
        teacher.setHireDate(LocalDate.of(2024, 1, 1));
        teacher.setStatus(TeacherStatus.ACTIVE);
        teacher.setAddress("School Campus");
        teacher.setEmail(email);
        teacher.setUser(user);
        return teacherRepository.save(teacher).getId();
    }

    private Long createLearner(String admissionNumber, Long schoolClassId) {
        Student student = new Student();
        student.setAdmissionNumber(admissionNumber);
        student.setFirstName("Learner");
        student.setLastName("One");
        student.setGender("FEMALE");
        student.setDateOfBirth(LocalDate.of(2012, 1, 2));
        student.setGrade(classRepository.findById(schoolClassId).orElseThrow().getLevel());
        student.setEnrollmentDate(LocalDate.of(2025, 1, 1));
        student.setGuardianName("Guardian");
        student.setGuardianRelationship("MOTHER");
        student.setGuardianPhone("5550000");
        student.setAddress("Address");
        student.setStatus(StudentStatus.ACTIVE);
        student.setSchoolClass(classRepository.findById(schoolClassId).orElseThrow());
        return studentRepository.save(student).getId();
    }

    private Long createSubject(String code, String name) {
        Subject subject = new Subject();
        subject.setCode(code);
        subject.setName(name);
        return subjectRepository.save(subject).getId();
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
