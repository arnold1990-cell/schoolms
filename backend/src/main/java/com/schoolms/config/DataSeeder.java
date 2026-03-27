package com.schoolms.config;

import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@schoolms.com").ifPresentOrElse(admin -> {
            boolean passwordMismatch = !passwordEncoder.matches("Admin123!", admin.getPassword());
            boolean roleMismatch = admin.getRole() != Role.ADMIN;
            boolean disabled = !admin.isEnabled();

            if (passwordMismatch || roleMismatch || disabled) {
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
            }
        }, () -> {
            User admin = new User();
            admin.setEmail("admin@schoolms.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        });
    }
}
