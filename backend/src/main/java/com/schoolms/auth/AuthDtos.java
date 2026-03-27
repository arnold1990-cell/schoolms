package com.schoolms.auth;

import com.schoolms.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record LoginRequest(@Email String email, @NotBlank String password) {}
    public record LoginResponse(String accessToken, String email, Role role) {}
    public record MeResponse(Long id, String email, Role role) {}
}
