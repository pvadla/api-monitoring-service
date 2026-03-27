package com.api.monitor.api;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.CreateEndpointRequest;
import com.api.monitor.api.dto.EndpointCheckResponse;
import com.api.monitor.api.dto.EndpointDetailResponse;
import com.api.monitor.api.dto.EndpointResponse;
import com.api.monitor.api.dto.UpdateEndpointRequest;
import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.SslMonitorRepository;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.EndpointDeletionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/endpoints")
@RequiredArgsConstructor
public class EndpointsApiController {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;
    private final SslMonitorRepository sslMonitorRepository;
    private final UserRepository userRepository;
    private final EndpointDeletionService endpointDeletionService;

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return endpointRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(this::toDetailResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody CreateEndpointRequest body) {

        if (body == null || body.name() == null || body.name().isBlank()
                || body.url() == null || body.url().isBlank()
                || body.checkInterval() == null) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("name, url, and checkInterval are required"));
        }

        User user = requireUser(principal);

        // FREE tier: max 5 monitors total (HTTP + heartbeat + SSL)
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

        Endpoint endpoint = new Endpoint();
        endpoint.setUser(user);
        endpoint.setName(body.name().trim());
        endpoint.setUrl(body.url().trim());
        endpoint.setCheckInterval(body.checkInterval());
        endpoint.setExpectedBodySubstring(
                body.expectedBodySubstring() != null && !body.expectedBodySubstring().isBlank()
                        ? body.expectedBodySubstring().trim()
                        : null);

        endpointRepository.save(endpoint);
        return ResponseEntity.status(HttpStatus.CREATED).body(EndpointResponse.fromEntity(endpoint));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            @RequestBody UpdateEndpointRequest body) {

        if (body == null || body.name() == null || body.name().isBlank()
                || body.url() == null || body.url().isBlank()
                || body.checkInterval() == null) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("name, url, and checkInterval are required"));
        }

        User user = requireUser(principal);
        return endpointRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(endpoint -> {
                    endpoint.setName(body.name().trim());
                    endpoint.setUrl(body.url().trim());
                    endpoint.setCheckInterval(body.checkInterval());
                    endpoint.setExpectedBodySubstring(
                            body.expectedBodySubstring() != null && !body.expectedBodySubstring().isBlank()
                                    ? body.expectedBodySubstring().trim()
                                    : null);
                    endpointRepository.save(endpoint);
                    return ResponseEntity.ok(EndpointResponse.fromEntity(endpoint));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        try {
            endpointDeletionService.deleteOwnedEndpoint(id, user);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    @PostMapping("/{id}/toggle")
    public ResponseEntity<?> toggle(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return endpointRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(endpoint -> {
                    endpoint.setIsActive(!Boolean.TRUE.equals(endpoint.getIsActive()));
                    endpointRepository.save(endpoint);
                    return ResponseEntity.ok(EndpointResponse.fromEntity(endpoint));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/toggle-status-visibility")
    public ResponseEntity<?> toggleStatusVisibility(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = requireUser(principal);
        return endpointRepository.findById(id)
                .filter(e -> e.getUser().getId().equals(user.getId()))
                .map(endpoint -> {
                    boolean currentlyShown = Boolean.TRUE.equals(endpoint.getShowOnStatusPage());
                    endpoint.setShowOnStatusPage(!currentlyShown);
                    endpointRepository.save(endpoint);
                    return ResponseEntity.ok(EndpointResponse.fromEntity(endpoint));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private EndpointDetailResponse toDetailResponse(Endpoint endpoint) {
        List<EndpointCheck> checks =
                endpointCheckRepository.findTop50ByEndpointOrderByCheckedAtDesc(endpoint);
        Collections.reverse(checks);

        long total = endpointCheckRepository.countByEndpoint(endpoint);
        long upCount = endpointCheckRepository.countByEndpointAndIsUp(endpoint, true);
        double uptimePct = total == 0 ? 100.0 : (upCount * 100.0 / total);

        double avgResponseMs = checks.stream()
                .filter(c -> c.getResponseTimeMs() != null)
                .mapToLong(c -> c.getResponseTimeMs())
                .average()
                .orElse(0.0);

        List<EndpointCheckResponse> checkDtos = checks.stream()
                .map(EndpointCheckResponse::fromEntity)
                .toList();

        return new EndpointDetailResponse(
                EndpointResponse.fromEntity(
                        endpoint,
                        EndpointResponse.recentChecksUpFromRows(
                                endpointCheckRepository.findTop15ByEndpointOrderByCheckedAtDesc(endpoint))),
                checkDtos,
                String.format("%.2f", uptimePct),
                String.format("%.0f", avgResponseMs));
    }

    private User requireUser(OAuth2User principal) {
        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            throw new IllegalStateException("missing email");
        }
        return userRepository.findByEmail(email).orElseThrow();
    }
}
