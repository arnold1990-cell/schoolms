package com.schoolms.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createUser("admin@schoolms.com", "Admin123!", Role.ADMIN);
        createUser("teacher@schoolms.com", "Teacher123!", Role.TEACHER);
    }

    @Test
    void adminCanAccessAdminOnlyEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(get("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void teacherCannotAccessAdminOnlyEndpoint() throws Exception {
        String teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");

        mockMvc.perform(get("/api/teachers")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanAccessTeacherAllowedEndpoint() throws Exception {
        String teacherToken = loginAndGetToken("teacher@schoolms.com", "Teacher123!");

        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserCannotAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void invalidTokenCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenCanAccessMeEndpoint() throws Exception {
        String adminToken = loginAndGetToken("admin@schoolms.com", "Admin123!");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
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
