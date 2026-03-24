package com.api.monitor.api;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.CreateHeartbeatRequest;
import com.api.monitor.api.dto.HeartbeatMonitorResponse;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatCheckRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/heartbeats")
@RequiredArgsConstructor
public class HeartbeatsApiController {

    private final HeartbeatMonitorRepository heartbeatRepository;
    private final EndpointRepository endpointRepository;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final HeartbeatCheckRepository heartbeatCheckRepository;

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody CreateHeartbeatRequest body) {

        if (body == null || body.name() == null || body.name().isBlank()
                || body.expectedIntervalMinutes() == null || body.expectedIntervalMinutes() < 1) {
            return ResponseEntity.badRequest()
                    .body(ApiMessageResponse.error("name and expectedIntervalMinutes (>=1) are required"));
        }

        User user = requireUser(principal);

        String tier = user.getSubscriptionTier();
        if (tier == null || tier.equalsIgnoreCase("FREE")) {
            long endpoints = endpointRepository.countByUser(user);
            long heartbeats = heartbeatRepository.findByUser(user).size();
            if (endpoints + heartbeats >= 5) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiMessageResponse.error(
                                "Free plan limit reached: you can have up to 5 monitors (HTTP + heartbeat)."));
            }
        }

        HeartbeatMonitor hb = new HeartbeatMonitor();
        hb.setUser(user);
        hb.setName(body.name().trim());
        hb.setExpectedIntervalMinutes(body.expectedIntervalMinutes());
        hb.setToken(UUID.randomUUID().toString().replace("-", ""));
        heartbeatRepository.save(hb);

        return ResponseEntity.status(HttpStatus.CREATED).body(HeartbeatMonitorResponse.fromEntity(hb));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return heartbeatRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .map(h -> {
                    // Close any open incidents before removing the monitor
                    incidentRepository.findByHeartbeatMonitorAndResolvedAtIsNull(h)
                            .forEach(incident -> {
                                incident.setResolvedAt(java.time.LocalDateTime.now());
                                incident.setStatus(Incident.IncidentStatus.RESOLVED);
                                incidentRepository.save(incident);
                            });
                    heartbeatCheckRepository.deleteByHeartbeatMonitor(h);
                    heartbeatRepository.delete(h);
                    return ResponseEntity.noContent().build();
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
