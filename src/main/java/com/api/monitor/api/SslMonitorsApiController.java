package com.api.monitor.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.CreateSslMonitorRequest;
import com.api.monitor.api.dto.SslMonitorResponse;
import com.api.monitor.entity.SslMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.SslCheckRepository;
import com.api.monitor.repository.SslMonitorRepository;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.MonitoringService;
import com.api.monitor.service.SslMonitorDeletionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/ssl-monitors")
@RequiredArgsConstructor
public class SslMonitorsApiController {

    private final SslMonitorRepository sslMonitorRepository;
    private final SslCheckRepository sslCheckRepository;
    private final EndpointRepository endpointRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;
    private final UserRepository userRepository;
    private final SslMonitorDeletionService sslMonitorDeletionService;
    private final MonitoringService monitoringService;

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody CreateSslMonitorRequest body) {

        if (body == null || body.name() == null || body.name().isBlank()
                || body.domain() == null || body.domain().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiMessageResponse.error("name and domain are required"));
        }

        User user = requireUser(principal);

        // FREE tier: total monitors (HTTP + heartbeat + SSL) capped at 5
        String tier = user.getSubscriptionTier();
        if (tier == null || tier.equalsIgnoreCase("FREE")) {
            long endpoints = endpointRepository.countByUser(user);
            long heartbeats = heartbeatMonitorRepository.findByUser(user).size();
            long sslMonitors = sslMonitorRepository.findByUser(user).size();
            if (endpoints + heartbeats + sslMonitors >= 5) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiMessageResponse.error(
                                "Free plan limit reached: you can have up to 5 monitors total (HTTP + heartbeat + SSL)."));
            }
        }

        String domain = body.domain().trim()
                .replaceFirst("(?i)^https?://", "") // strip any accidentally pasted scheme
                .replaceFirst("/.*$", "");           // strip path

        SslMonitor monitor = new SslMonitor();
        monitor.setUser(user);
        monitor.setName(body.name().trim());
        monitor.setDomain(domain);
        monitor.setPort(body.port() != null && body.port() > 0 ? body.port() : 443);
        monitor.setAlertDaysThreshold(
                body.alertDaysThreshold() != null && body.alertDaysThreshold() > 0
                        ? body.alertDaysThreshold() : 30);
        sslMonitorRepository.save(monitor);
        try {
            monitoringService.checkSslMonitorNow(monitor);
        } catch (Exception ex) {
            log.warn("Initial SSL check failed for monitor {}: {}", monitor.getId(), ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SslMonitorResponse.fromEntity(sslMonitorRepository.findById(monitor.getId()).orElse(monitor)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return sslMonitorRepository.findById(id)
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .map(m -> {
                    List<Boolean> sparkline = SslMonitorResponse.recentChecksUpFromRows(
                            sslCheckRepository.findTop15BySslMonitorOrderByCheckedAtDesc(m));
                    return ResponseEntity.ok(SslMonitorResponse.fromEntity(m, sparkline));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        try {
            sslMonitorDeletionService.deleteOwnedSslMonitor(id, user);
            return ResponseEntity.noContent().build();
        } catch (org.springframework.web.server.ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    private User requireUser(OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) throw new IllegalStateException("missing email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
