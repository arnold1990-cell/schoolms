package com.schoolms.teacher;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        subjectRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setEmail("admin@schoolms.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);

        adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");
    }

    @Test
    void createTeacherSuccess() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1001", "teacher1001@schoolms.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-1001"))
                .andExpect(jsonPath("$.data.department").value("Sciences"));
    }

    @Test
    void createTeacherValidationFailure() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employeeNumber": "",
                                  "firstName": "",
                                  "lastName": "",
                                  "phoneNumber": "",
                                  "email": "invalid",
                                  "department": "",
                                  "specialization": "",
                                  "hireDate": null,
                                  "status": null,
                                  "gender": null,
                                  "employmentType": null,
                                  "address": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createTeacherDuplicateEmployeeNumber() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1002", "teacher1002@schoolms.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1002", "teacher1003@schoolms.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee number already exists"));
    }

    @Test
    void createTeacherDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1004", "teacher1004@schoolms.com")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1005", "teacher1004@schoolms.com")))
                .andExpect(status().isConflict());
    }

    @Test
    void createTeacherLinksExistingTeacherUserAccount() throws Exception {
        User orphanTeacherUser = new User();
        orphanTeacherUser.setEmail("teacher@schoolms.com");
        orphanTeacherUser.setPassword(passwordEncoder.encode("Teacher123!"));
        orphanTeacherUser.setRole(Role.TEACHER);
        orphanTeacherUser.setEnabled(true);
        orphanTeacherUser = userRepository.save(orphanTeacherUser);

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-LINK", "teacher@schoolms.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("teacher@schoolms.com"));

        org.assertj.core.api.Assertions.assertThat(teacherRepository.findByUserId(orphanTeacherUser.getId())).isPresent();
    }

    @Test
    void fetchTeacherById() throws Exception {
        String createdJson = mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1006", "teacher1006@schoolms.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = com.jayway.jsonpath.JsonPath.read(createdJson, "$.data.id");

        mockMvc.perform(get("/api/teachers/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void updateTeacher() throws Exception {
        String createdJson = mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1007", "teacher1007@schoolms.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = com.jayway.jsonpath.JsonPath.read(createdJson, "$.data.id");

        mockMvc.perform(put("/api/teachers/{id}", id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1007", "teacher1007-updated@schoolms.com").replace("Physics", "Chemistry")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("teacher1007-updated@schoolms.com"))
                .andExpect(jsonPath("$.data.specialization").value("Chemistry"));
    }

    @Test
    void deactivateTeacherOnDelete() throws Exception {
        String createdJson = mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(basePayload("EMP-1008", "teacher1008@schoolms.com")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = com.jayway.jsonpath.JsonPath.read(createdJson, "$.data.id");

        mockMvc.perform(delete("/api/teachers/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teachers/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));
    }

    private String basePayload(String employeeNumber, String email) {
        return """
                {
                  "employeeNumber":"%s",
                  "firstName":"Jane",
                  "middleName":"M",
                  "lastName":"Doe",
                  "gender":"FEMALE",
                  "dateOfBirth":"1990-06-20",
                  "phoneNumber":"+1-555-1000",
                  "alternativePhoneNumber":"+1-555-1001",
                  "email":"%s",
                  "nationalId":"NID-%s",
                  "passportNumber":"P-%s",
                  "department":"Sciences",
                  "specialization":"Physics",
                  "employmentType":"FULL_TIME",
                  "hireDate":"2020-08-15",
                  "status":"ACTIVE",
                  "address":"Main Street 100",
                  "title":"MS",
                  "emergencyContactName":"John Doe",
                  "emergencyContactPhone":"+1-555-2000",
                  "emergencyContactRelationship":"Brother",
                  "qualification":"B.Ed",
                  "highestEducationLevel":"Masters",
                  "yearsOfExperience":6,
                  "staffRole":"Senior Teacher",
                  "salaryGrade":"G7",
                  "notes":"Handles science labs",
                  "profilePhotoUrl":"https://cdn.schoolms.local/teacher.jpg",
                  "password":"Teacher123!",
                  "enabled":true
                }
                """.formatted(employeeNumber, email, employeeNumber, employeeNumber);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"%s"}
                                        """.formatted(email, password)))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.accessToken"
        );
    }
}
