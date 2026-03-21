package com.api.monitor.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.StatusSettingsResponse;
import com.api.monitor.api.dto.StatusSettingsUpdateRequest;
import com.api.monitor.entity.User;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/status")
@RequiredArgsConstructor
public class SettingsStatusApiController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<StatusSettingsResponse> get(@AuthenticationPrincipal OAuth2User principal) {
        User user = requireUser(principal);
        return ResponseEntity.ok(StatusSettingsResponse.fromEntity(user));
    }

    @PutMapping
    public ResponseEntity<?> put(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody(required = false) StatusSettingsUpdateRequest body) {

        if (body == null) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("request body required"));
        }

        User user = requireUser(principal);
        if (body.statusSlug() != null && !body.statusSlug().isBlank()) {
            String statusSlug = body.statusSlug().trim().toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
            if (!statusSlug.isBlank()) {
                user.setStatusSlug(statusSlug);
            }
        }
        user.setStatusPageTitle(
                body.statusPageTitle() != null && !body.statusPageTitle().isBlank()
                        ? body.statusPageTitle().trim()
                        : null);
        user.setStatusPageLogoUrl(
                body.statusPageLogoUrl() != null && !body.statusPageLogoUrl().isBlank()
                        ? body.statusPageLogoUrl().trim()
                        : null);
        userRepository.save(user);
        return ResponseEntity.ok(StatusSettingsResponse.fromEntity(user));
    }

    private User requireUser(OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            throw new IllegalStateException("missing email");
        }
        return userRepository.findByEmail(email).orElseThrow();
    }
}
