package com.schoolms.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolms.classmanagement.SchoolClass;
import com.schoolms.classmanagement.SchoolClassRepository;
import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
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
        schoolClass.setName("Standard 5");
        schoolClass.setStream("A");
        classId = classRepository.save(schoolClass).getId();
    }

    @Test
    void createStudentWithCoreRequiredFieldsPasses() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corePayload("ADM-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value("ADM-001"));
    }

    @Test
    void createStudentWithClassIdAliasPasses() throws Exception {
        Map<String, Object> payload = corePayload("ADM-CLASS-ALIAS");
        payload.remove("schoolClassId");
        payload.put("classId", classId);

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.schoolClassId").value(classId));
    }

    @Test
    void createStudentWithAllFieldsPasses() throws Exception {
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fullPayload("ADM-ALL-001"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.guardianPhone").value("555-0001"))
                .andExpect(jsonPath("$.data.schoolClassId").value(classId));
    }

    @Test
    void createStudentMissingFirstNameFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-FN");
        payload.remove("firstName");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists());
    }

    @Test
    void createStudentMissingLastNameFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-LN");
        payload.remove("lastName");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.lastName").exists());
    }

    @Test
    void createStudentMissingAdmissionNumberFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-ADM");
        payload.remove("admissionNumber");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.admissionNumber").exists());
    }

    @Test
    void createStudentMissingGenderFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-GENDER");
        payload.remove("gender");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.gender").exists());
    }

    @Test
    void createStudentMissingGradeFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-GRADE");
        payload.remove("grade");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.grade").exists());
    }

    @Test
    void createStudentMissingEnrollmentDateFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-ENR");
        payload.remove("enrollmentDate");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.enrollmentDate").exists());
    }

    @Test
    void createStudentMissingStatusFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-MISS-STATUS");
        payload.remove("status");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").exists());
    }

    @Test
    void createStudentWithDuplicateAdmissionNumberFails() throws Exception {
        mockMvc.perform(post("/api/students")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(corePayload("ADM-DUP-1"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(corePayload("ADM-DUP-1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Admission number already exists"));
    }

    @Test
    void createStudentWithNullSchoolClassFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-NO-CLASS");
        payload.put("schoolClassId", null);

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.schoolClassId").exists());
    }

    @Test
    void createStudentWithOptionalFieldsNullOrBlankPasses() throws Exception {
        Map<String, Object> payload = corePayload("ADM-OPT-NULL");
        payload.put("middleName", "");
        payload.put("preferredName", " ");
        payload.put("dateOfBirth", null);
        payload.put("guardianName", "");
        payload.put("guardianRelationship", " ");
        payload.put("guardianPhone", "");
        payload.put("address", " ");
        payload.put("nationality", "");
        payload.put("notes", " ");
        payload.put("phoneNumber", "");
        payload.put("email", "");
        payload.put("medicalConditions", " ");
        payload.put("usesTransport", null);
        payload.put("pickupPoint", " ");
        payload.put("routeName", "");

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.admissionNumber").value("ADM-OPT-NULL"))
                .andExpect(jsonPath("$.data.dateOfBirth").isEmpty())
                .andExpect(jsonPath("$.data.guardianName").isEmpty());
    }

    @Test
    void updateStudentWithOptionalFieldsEmptyPasses() throws Exception {
        long studentId = createStudentAndGetId(corePayload("ADM-UPD-1"));
        Map<String, Object> payload = corePayload("ADM-UPD-1");
        payload.put("middleName", "");
        payload.put("notes", " ");

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.middleName").isEmpty())
                .andExpect(jsonPath("$.data.notes").isEmpty());
    }

    @Test
    void deleteStudentWithMatchingConfirmationFieldsPasses() throws Exception {
        Map<String, Object> payload = corePayload("ADM-DEL-1");
        long studentId = createStudentAndGetId(payload);

        mockMvc.perform(delete("/api/students/{id}/confirm-delete", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deletePayload(payload))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteStudentWithMismatchedConfirmationFieldFails() throws Exception {
        Map<String, Object> payload = corePayload("ADM-DEL-2");
        long studentId = createStudentAndGetId(payload);

        Map<String, Object> confirmation = deletePayload(payload);
        confirmation.put("grade", "Standard 6");

        mockMvc.perform(delete("/api/students/{id}/confirm-delete", studentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Student delete confirmation failed. Core student details do not match."));
    }

    private long createStudentAndGetId(Map<String, Object> payload) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("id").asLong();
    }

    private Map<String, Object> corePayload(String admissionNumber) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName", "John");
        payload.put("lastName", "Doe");
        payload.put("admissionNumber", admissionNumber);
        payload.put("gender", "MALE");
        payload.put("grade", "Standard 5");
        payload.put("schoolClassId", classId);
        payload.put("enrollmentDate", "2026-03-30");
        payload.put("status", "ACTIVE");
        return payload;
    }

    private Map<String, Object> fullPayload(String admissionNumber) {
        Map<String, Object> payload = corePayload(admissionNumber);
        payload.put("middleName", "A");
        payload.put("preferredName", "JD");
        payload.put("dateOfBirth", "2014-02-01");
        payload.put("guardianName", "Jane Doe");
        payload.put("guardianRelationship", "Mother");
        payload.put("guardianPhone", "555-0001");
        payload.put("address", "Main Street");
        payload.put("nationality", "Zimbabwean");
        payload.put("nationalId", "12-345678Z12");
        payload.put("passportNumber", "PA123456");
        payload.put("previousSchool", "City Prep");
        payload.put("phoneNumber", "555-0010");
        payload.put("alternativePhoneNumber", "555-0011");
        payload.put("email", "john@example.com");
        payload.put("addressLine1", "Line 1");
        payload.put("addressLine2", "Line 2");
        payload.put("city", "Harare");
        payload.put("district", "CBD");
        payload.put("postalCode", "0001");
        payload.put("country", "Zimbabwe");
        payload.put("guardianAltPhone", "555-0012");
        payload.put("guardianEmail", "guardian@example.com");
        payload.put("guardianOccupation", "Engineer");
        payload.put("guardianAddress", "Guardian Address");
        payload.put("emergencyContactName", "Uncle Joe");
        payload.put("emergencyContactPhone", "555-0020");
        payload.put("emergencyContactRelationship", "Uncle");
        payload.put("bloodGroup", "O+");
        payload.put("allergies", "Peanuts");
        payload.put("medicalConditions", "Asthma");
        payload.put("disabilities", "None");
        payload.put("medication", "Inhaler");
        payload.put("hospitalName", "Central Hospital");
        payload.put("doctorName", "Dr. Smith");
        payload.put("doctorPhone", "555-0030");
        payload.put("usesTransport", true);
        payload.put("pickupPoint", "Gate A");
        payload.put("routeName", "Route 2");
        payload.put("driverAssignment", "Driver A");
        payload.put("religion", "Christianity");
        payload.put("homeLanguage", "English");
        payload.put("residencyType", "Day Scholar");
        payload.put("sponsorshipStatus", "Self");
        payload.put("feeCategory", "Regular");
        payload.put("notes", "N/A");
        payload.put("schoolClassId", classId);
        return payload;
    }

    private Map<String, Object> deletePayload(Map<String, Object> sourcePayload) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("firstName", sourcePayload.get("firstName"));
        payload.put("lastName", sourcePayload.get("lastName"));
        payload.put("admissionNumber", sourcePayload.get("admissionNumber"));
        payload.put("gender", sourcePayload.get("gender"));
        payload.put("grade", sourcePayload.get("grade"));
        payload.put("enrollmentDate", sourcePayload.get("enrollmentDate"));
        payload.put("status", sourcePayload.get("status"));
        return payload;
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
