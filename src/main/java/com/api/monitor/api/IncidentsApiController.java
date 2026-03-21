package com.api.monitor.api;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.IncidentResponse;
import com.api.monitor.api.dto.IncidentStatusUpdateRequest;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.Incident.IncidentStatus;
import com.api.monitor.entity.User;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentsApiController {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> list(@AuthenticationPrincipal OAuth2User principal) {
        User user = requireUser(principal);
        List<Incident> incidents = incidentRepository.findLatestByUser(user, 100);
        return ResponseEntity.ok(incidents.stream().map(IncidentResponse::fromEntity).toList());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return incidentRepository.findById(id)
                .filter(i -> i.getUser().getId().equals(user.getId()))
                .map(incident -> {
                    incident.setStatus(IncidentStatus.RESOLVED);
                    incident.setResolvedAt(LocalDateTime.now());
                    incidentRepository.save(incident);
                    return ResponseEntity.ok(IncidentResponse.fromEntity(incident));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            @RequestBody IncidentStatusUpdateRequest body) {

        if (body == null || body.status() == null || body.status().isBlank()) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("status is required"));
        }

        IncidentStatus status;
        try {
            status = IncidentStatus.valueOf(body.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("invalid status"));
        }

        User user = requireUser(principal);
        return incidentRepository.findById(id)
                .filter(i -> i.getUser().getId().equals(user.getId()))
                .map(incident -> {
                    incident.setStatus(status);
                    if (status == IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
                        incident.setResolvedAt(LocalDateTime.now());
                    }
                    incidentRepository.save(incident);
                    return ResponseEntity.ok(IncidentResponse.fromEntity(incident));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private User requireUser(OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            throw new IllegalStateException("missing email");
        }
        return userRepository.findByEmail(email).orElseThrow();
    }
}
