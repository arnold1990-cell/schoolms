package com.schoolms.auth;

import com.schoolms.common.AppException;
import com.schoolms.security.JwtService;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        String token = jwtService.generateToken(user.getEmail(), java.util.Map.of("role", user.getRole().name(), "uid", user.getId()));
        return new AuthDtos.LoginResponse(token, "Bearer", new AuthDtos.AuthUser(user.getId(), user.getEmail(), user.getRole()));
    }

    public AuthDtos.MeResponse me(String email) {
        var user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return new AuthDtos.MeResponse(user.getId(), user.getEmail(), user.getRole());
    }
}
