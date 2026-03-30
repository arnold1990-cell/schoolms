package com.schoolms.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SchoolClassRepository classRepository;

    private String adminToken;
    private Long classId;

    @BeforeEach
    void setUp() throws Exception {
        studentRepository.deleteAll();
        classRepository.deleteAll();
        userRepository.deleteAll();

        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setName("Grade 7");
        schoolClass.setStream("A");
        classId = classRepository.save(schoolClass).getId();
    }

    @Test
    void createStudentSuccess() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("ADM-100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value("ADM-100"))
                .andExpect(jsonPath("$.data.guardianPhone").value("555-0001"));
    }

    @Test
    void createStudentValidationFailure() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.admissionNumber").exists());
    }

    @Test
    void createStudentValidationFailureWhenRequiredFieldMissing() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithoutFirstName("ADM-109")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.firstName").exists());
    }


    @Test
    void createStudentValidationFailureWhenEnrollmentDateMissing() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithoutEnrollmentDate("ADM-105")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.enrollmentDate").exists());
    }

    @Test
    void createStudentDuplicateAdmissionNumber() throws Exception {
        mockMvc.perform(post("/api/students")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validPayload("ADM-101"))).andExpect(status().isOk());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("ADM-101")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Admission number already exists"));
    }

    @Test
    void createStudentInvalidClassId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithClassId("ADM-106", 999999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Class not found"));
    }

    @Test
    void createStudentInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithInvalidDate("ADM-107")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid date value. Use yyyy-MM-dd format"));
    }

    @Test
    void createStudentInvalidEnumValue() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithInvalidStatus("ADM-108")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status value"));
    }

    @Test
    void createStudentInvalidGenderEnum() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayloadWithInvalidGender("ADM-110")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.gender").value("Gender must be one of: MALE, FEMALE, OTHER"));
    }

    @Test
    void fetchStudentById() throws Exception {
        long studentId = createStudentAndGetId("ADM-102");

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(studentId));
    }

    @Test
    void updateStudent() throws Exception {
        long studentId = createStudentAndGetId("ADM-103");

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("ADM-103-UPDATED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admissionNumber").value("ADM-103-UPDATED"));
    }

    @Test
    void deleteStudent() throws Exception {
        long studentId = createStudentAndGetId("ADM-104");

        mockMvc.perform(delete("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private long createStudentAndGetId(String admissionNumber) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload(admissionNumber)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode payload = objectMapper.readTree(result.getResponse().getContentAsString());
        return payload.path("data").path("id").asLong();
    }


    private String validPayloadWithoutEnrollmentDate(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "2013-05-12",
                  "grade": "Grade 7",
                  "classId": %d,
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
    }

    private String validPayloadWithoutFirstName(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "2013-05-12",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
    }

    private String validPayloadWithClassId(String admissionNumber, long requestedClassId) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "2013-05-12",
                  "grade": "Grade 7",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, requestedClassId);
    }

    private String validPayloadWithInvalidDate(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "05/12/2013",
                  "grade": "Grade 7",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
    }

    private String validPayloadWithInvalidStatus(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "2013-05-12",
                  "grade": "Grade 7",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVEE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
    }

    private String validPayloadWithInvalidGender(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMME",
                  "dateOfBirth": "2013-05-12",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
    }

    private String validPayload(String admissionNumber) {
        return """
                {
                  "admissionNumber": "%s",
                  "firstName": "Jane",
                  "lastName": "Doe",
                  "gender": "FEMALE",
                  "dateOfBirth": "2013-05-12",
                  "grade": "Grade 7",
                  "classId": %d,
                  "enrollmentDate": "2025-01-06",
                  "guardianName": "John Doe",
                  "guardianRelationship": "FATHER",
                  "guardianPhone": "555-0001",
                  "address": "Central Road",
                  "status": "ACTIVE",
                  "email": "jane.doe@example.com"
                }
                """.formatted(admissionNumber, classId);
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
