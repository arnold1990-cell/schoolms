package com.schoolms.config;

import com.schoolms.user.Role;
import com.schoolms.user.User;
import com.schoolms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        userRepository.findByEmail("admin@schoolms.com").ifPresentOrElse(admin -> {
            boolean passwordMatches = passwordEncoder.matches("Admin123!", admin.getPassword());
            boolean passwordMismatch = !passwordMatches;
            boolean roleMismatch = admin.getRole() != Role.ADMIN;
            boolean disabled = !admin.isEnabled();

            log.info("Admin seed check: email={}, passwordMatches={}, role={}, enabled={}",
                    admin.getEmail(), passwordMatches, admin.getRole(), admin.isEnabled());

            if (passwordMismatch || roleMismatch || disabled) {
                admin.setPassword(passwordEncoder.encode("Admin123!"));
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepository.save(admin);
                log.warn("Admin account data mismatch detected. Password/role/enabled values were reset to defaults.");
            }
        }, () -> {
            User admin = new User();
            admin.setEmail("admin@schoolms.com");
            admin.setPassword(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("Admin account created with default credentials.");
        });
    }
}
