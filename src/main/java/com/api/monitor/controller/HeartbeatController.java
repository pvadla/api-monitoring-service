package com.api.monitor.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatCheckRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.MonitoringService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HeartbeatController {

    private final HeartbeatMonitorRepository heartbeatRepository;
    private final EndpointRepository endpointRepository;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final HeartbeatCheckRepository heartbeatCheckRepository;
    private final MonitoringService monitoringService;

    @GetMapping("/heartbeats")
    public String list() {
        return "redirect:/dashboard";
    }

    @PostMapping("/heartbeats")
    public String create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String name,
            @RequestParam Integer expectedIntervalMinutes,
            RedirectAttributes redirectAttributes) {

        User user = getUser(principal);

        // Enforce FREE tier limit: max 5 monitors total (endpoints + heartbeats)
        String tier = user.getSubscriptionTier();
        if (tier == null || tier.equalsIgnoreCase("FREE")) {
            long endpoints = endpointRepository.countByUser(user);
            long heartbeats = heartbeatRepository.findByUser(user).size();
            if (endpoints + heartbeats >= 5) {
                redirectAttributes.addFlashAttribute("error",
                        "Free plan limit reached: you can have up to 5 monitors (HTTP + heartbeat). Remove one or upgrade your plan.");
                return "redirect:/dashboard";
            }
        }

        HeartbeatMonitor hb = new HeartbeatMonitor();
        hb.setUser(user);
        hb.setName(name.trim());
        hb.setExpectedIntervalMinutes(expectedIntervalMinutes);
        hb.setToken(UUID.randomUUID().toString().replace("-", ""));
        heartbeatRepository.save(hb);

        return "redirect:/dashboard";
    }

    @PostMapping("/heartbeats/{id}/delete")
    public String delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id) {

        User user = getUser(principal);
        heartbeatRepository.findById(id)
                .filter(h -> h.getUser().getId().equals(user.getId()))
                .ifPresent(h -> {
                    // Close any open incidents before removing the monitor
                    incidentRepository.findByHeartbeatMonitorAndResolvedAtIsNull(h)
                            .forEach(incident -> {
                                incident.setResolvedAt(java.time.LocalDateTime.now());
                                incident.setStatus(Incident.IncidentStatus.RESOLVED);
                                incidentRepository.save(incident);
                            });
                    heartbeatCheckRepository.deleteByHeartbeatMonitor(h);
                    heartbeatRepository.delete(h);
                });

        return "redirect:/dashboard";
    }

    /**
     * Public heartbeat endpoint. Called by cron jobs / workers to signal success.
     * Accepts both GET and POST to make integration easy.
     */
    @RequestMapping(path = "/heartbeat/{token}")
    @ResponseBody
    public String ping(@PathVariable String token) {
        return heartbeatRepository.findByToken(token)
                .map(hb -> {
                    hb.setLastPingAt(LocalDateTime.now());
                    hb.setIsUp(true);
                    hb.setLastNotifiedAt(null); // clear so next miss will re-alert
                    heartbeatRepository.save(hb);
                    // Resolve any open incident and send recovery email
                    monitoringService.resolveHeartbeatIncident(hb);
                    return "OK";
                })
                .orElse("Unknown heartbeat token");
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}

