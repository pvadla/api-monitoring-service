package com.api.monitor.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.DashboardResponse;
import com.api.monitor.api.dto.EndpointResponse;
import com.api.monitor.api.dto.HeartbeatMonitorResponse;
import com.api.monitor.api.dto.UserResponse;
import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardApiController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;

    @Value("${apiwatch.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(value = "subscription", required = false) String subscription) {

        String email = principal != null ? principal.getAttribute("email") : null;
        if (email == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        long total = endpointRepository.countByUser(user);
        long upCount = endpointRepository.countByUserAndIsUp(user, true);
        long downCount = endpointRepository.countByUserAndIsUp(user, false);

        List<Endpoint> endpoints = endpointRepository.findByUser(user);
        List<HeartbeatMonitor> heartbeats = heartbeatMonitorRepository.findByUser(user);

        String flashSuccess = null;
        if ("success".equals(subscription)) {
            flashSuccess = "Subscription activated! You now have access to the STARTER plan.";
        }

        DashboardResponse body = new DashboardResponse(
                UserResponse.fromEntity(user),
                total,
                upCount,
                downCount,
                endpoints.stream()
                        .map(ep -> EndpointResponse.fromEntity(
                                ep,
                                EndpointResponse.recentChecksUpFromRows(
                                        endpointCheckRepository.findTop15ByEndpointOrderByCheckedAtDesc(ep))))
                        .toList(),
                heartbeats.stream().map(HeartbeatMonitorResponse::fromEntity).toList(),
                baseUrl,
                flashSuccess
        );
        return ResponseEntity.ok(body);
    }
}
