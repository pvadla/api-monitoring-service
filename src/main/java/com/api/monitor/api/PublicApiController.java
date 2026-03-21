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
import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;
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
    private final EmailNotificationService emailNotificationService;

    @GetMapping("/status/{slug}")
    public ResponseEntity<?> publicStatus(@PathVariable String slug) {
        return userRepository.findByStatusSlug(slug)
                .map(owner -> {
                    List<Endpoint> endpoints = endpointRepository.findByUserAndShowOnStatusPageTrue(owner);
                    List<Incident> incidents = incidentRepository.findLatestByUser(owner, 50);

                    String title = owner.getStatusPageTitle() != null && !owner.getStatusPageTitle().isBlank()
                            ? owner.getStatusPageTitle()
                            : "Status";
                    String logoUrl = owner.getStatusPageLogoUrl();

                    long upCount = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getIsUp())).count();
                    String overallStatusLabel = endpoints.isEmpty() ? "No endpoints configured"
                            : (upCount == endpoints.size() ? "All Systems Operational" : "Some Issues");
                    String statusKind = endpoints.isEmpty() ? "none" : (upCount == endpoints.size() ? "all-up" : "issues");

                    PublicStatusResponse dto = new PublicStatusResponse(
                            slug,
                            title,
                            logoUrl,
                            overallStatusLabel,
                            statusKind,
                            endpoints.stream().map(EndpointResponse::fromEntity).toList(),
                            incidents.stream().map(IncidentResponse::fromEntity).toList());
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
