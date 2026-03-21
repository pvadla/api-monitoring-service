package com.api.monitor.api;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.ProfileUpdateRequest;
import com.api.monitor.api.dto.UserResponse;
import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/settings/profile")
@RequiredArgsConstructor
public class SettingsProfileApiController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final IncidentRepository incidentRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;

    @GetMapping
    public ResponseEntity<UserResponse> get(@AuthenticationPrincipal OAuth2User principal) {
        User user = requireUser(principal);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PutMapping
    public ResponseEntity<?> put(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody(required = false) ProfileUpdateRequest body) {

        if (body == null) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("request body required"));
        }

        User user = requireUser(principal);
        if (body.name() != null) {
            user.setName(body.name().trim());
        }
        if (body.notifyOnEndpointDown() != null) {
            user.setNotifyOnEndpointDown(body.notifyOnEndpointDown());
        }
        if (body.notifyOnEndpointRecovery() != null) {
            user.setNotifyOnEndpointRecovery(body.notifyOnEndpointRecovery());
        }
        userRepository.save(user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PostMapping("/delete-account")
    @Transactional
    public ResponseEntity<Map<String, Boolean>> deleteAccount(
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = requireUser(principal);

        List<Endpoint> endpoints = endpointRepository.findByUser(user);
        if (!endpoints.isEmpty()) {
            endpointCheckRepository.deleteByEndpointIn(endpoints);
        }
        incidentRepository.deleteByUser(user);

        List<HeartbeatMonitor> heartbeats = heartbeatMonitorRepository.findByUser(user);
        if (!heartbeats.isEmpty()) {
            heartbeatMonitorRepository.deleteAll(heartbeats);
        }

        if (!endpoints.isEmpty()) {
            endpointRepository.deleteAll(endpoints);
        }

        userRepository.delete(user);

        new SecurityContextLogoutHandler().logout(request, response,
                SecurityContextHolder.getContext().getAuthentication());

        return ResponseEntity.ok(ApiMessageResponse.success());
    }

    private User requireUser(OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            throw new IllegalStateException("missing email");
        }
        return userRepository.findByEmail(email).orElseThrow();
    }
}
