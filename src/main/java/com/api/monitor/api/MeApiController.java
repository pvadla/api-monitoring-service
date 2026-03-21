package com.api.monitor.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.UserResponse;
import com.api.monitor.entity.User;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Session-based identity for the React SPA ({@code GET /api/me}).
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeApiController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        return userRepository.findByEmail(email)
                .map(u -> ResponseEntity.ok(UserResponse.fromEntity(u)))
                .orElse(ResponseEntity.status(401).build());
    }
}
