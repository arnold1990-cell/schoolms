package com.schoolms.notification;

import com.schoolms.common.ApiResponse;
import com.schoolms.common.AppException;
import com.schoolms.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<List<Notification>> list(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName()).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return ApiResponse.ok("Notifications", repository.findByRecipientIdOrderByCreatedAtDesc(user.getId()));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public ApiResponse<Notification> read(@PathVariable Long id) {
        Notification n = repository.findById(id).orElseThrow(() -> new AppException("Notification not found", HttpStatus.NOT_FOUND));
        n.setRead(true);
        return ApiResponse.ok("Notification updated", repository.save(n));
    }
}
