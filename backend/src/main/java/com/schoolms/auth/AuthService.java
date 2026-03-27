package com.schoolms.auth;

import com.schoolms.common.AppException;
import com.schoolms.security.JwtService;
import com.schoolms.user.Role;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        String normalizedEmail = request.email() == null ? null : request.email().trim();
        boolean userExists = normalizedEmail != null && userRepository.findByEmail(normalizedEmail).isPresent();
        log.info("Login attempt for email='{}', userExists={}", normalizedEmail, userExists);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Login failed for email='{}': {}", normalizedEmail, ex.getMessage());
            throw ex;
        }

        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.TEACHER) {
            throw new AppException("Role is not allowed to access this application", HttpStatus.FORBIDDEN);
        }
        String token = jwtService.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name(), "uid", user.getId()));
        log.info("Login successful for email='{}', role={}", user.getEmail(), user.getRole());
        return new AuthDtos.LoginResponse(token, user.getEmail(), user.getRole());
    }

    public AuthDtos.MeResponse me(String email) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return new AuthDtos.MeResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
