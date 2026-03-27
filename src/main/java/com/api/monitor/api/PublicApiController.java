package com.api.monitor.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.api.dto.ApiMessageResponse;
import com.api.monitor.api.dto.ContactRequest;
import com.api.monitor.api.dto.EndpointResponse;
import com.api.monitor.api.dto.IncidentResponse;
import com.api.monitor.api.dto.PublicStatusResponse;
import com.api.monitor.api.dto.SslMonitorResponse;
import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.SslMonitor;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.SslCheckRepository;
import com.api.monitor.repository.SslMonitorRepository;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.EmailNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicApiController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final IncidentRepository incidentRepository;
    private final SslMonitorRepository sslMonitorRepository;
    private final SslCheckRepository sslCheckRepository;
    private final EmailNotificationService emailNotificationService;

    @GetMapping("/status/{slug}")
    public ResponseEntity<?> publicStatus(@PathVariable String slug) {
        return userRepository.findByStatusSlug(slug)
                .map(owner -> {
                    List<Endpoint> endpoints = endpointRepository.findByUserAndShowOnStatusPageTrue(owner);
                    List<SslMonitor> sslMonitors = sslMonitorRepository.findByUserAndShowOnStatusPageTrue(owner);
                    List<Incident> incidents = incidentRepository.findLatestByUser(owner, 50);

                    String title = owner.getStatusPageTitle() != null && !owner.getStatusPageTitle().isBlank()
                            ? owner.getStatusPageTitle()
                            : "Status";
                    String logoUrl = owner.getStatusPageLogoUrl();

                    long epUpCount = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getIsUp())).count();
                    long sslUpCount = sslMonitors.stream().filter(s -> Boolean.TRUE.equals(s.getIsUp())).count();
                    long totalComponents = endpoints.size() + sslMonitors.size();
                    long totalUp = epUpCount + sslUpCount;
                    String overallStatusLabel = totalComponents == 0 ? "No components configured"
                            : (totalUp == totalComponents ? "All Systems Operational" : "Some Issues");
                    String statusKind = totalComponents == 0 ? "none" : (totalUp == totalComponents ? "all-up" : "issues");

                    List<SslMonitorResponse> sslResponses = sslMonitors.stream()
                            .map(s -> SslMonitorResponse.fromEntity(
                                    s,
                                    SslMonitorResponse.recentChecksUpFromRows(
                                            sslCheckRepository.findTop15BySslMonitorOrderByCheckedAtDesc(s))))
                            .toList();

                    List<EndpointResponse> epResponses = endpoints.stream()
                            .map(ep -> EndpointResponse.fromEntity(ep))
                            .toList();
                    List<IncidentResponse> incResponses = incidents.stream()
                            .map(inc -> IncidentResponse.fromEntity(inc))
                            .toList();

                    PublicStatusResponse dto = new PublicStatusResponse(
                            slug,
                            title,
                            logoUrl,
                            overallStatusLabel,
                            statusKind,
                            epResponses,
                            sslResponses,
                            incResponses);
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/contact")
    public ResponseEntity<?> contact(@RequestBody ContactRequest body) {
        if (body == null || body.name() == null || body.name().isBlank()
                || body.email() == null || body.email().isBlank()
                || body.message() == null || body.message().isBlank()) {
            return ResponseEntity.badRequest().body(ApiMessageResponse.error("name, email, and message are required"));
        }
        emailNotificationService.sendContactEmail(
                body.name().trim(),
                body.email().trim(),
                body.subject() != null ? body.subject().trim() : null,
                body.message().trim());
        return ResponseEntity.ok(ApiMessageResponse.success());
    }
}
