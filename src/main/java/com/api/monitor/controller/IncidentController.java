package com.api.monitor.controller;

import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.monitor.entity.Incident;
import com.api.monitor.entity.Incident.IncidentStatus;
import com.api.monitor.entity.User;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    @GetMapping
    public String list(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = getUser(principal);
        var incidents = incidentRepository.findLatestByUser(user, 100);
        model.addAttribute("user", user);
        model.addAttribute("incidents", incidents);
        return "incidents";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Incident incident = getOwnedIncident(id, principal);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(LocalDateTime.now());
        incidentRepository.save(incident);

        redirectAttributes.addFlashAttribute("success", "Incident marked resolved.");
        return "redirect:/incidents";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            @RequestParam IncidentStatus status,
            RedirectAttributes redirectAttributes) {

        Incident incident = getOwnedIncident(id, principal);
        incident.setStatus(status);
        if (status == IncidentStatus.RESOLVED && incident.getResolvedAt() == null) {
            incident.setResolvedAt(LocalDateTime.now());
        }
        incidentRepository.save(incident);

        redirectAttributes.addFlashAttribute("success", "Status updated.");
        return "redirect:/incidents";
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }

    private Incident getOwnedIncident(Long id, OAuth2User principal) {
        Incident incident = incidentRepository.findById(id).orElseThrow(() -> new RuntimeException("Incident not found"));
        User user = getUser(principal);
        if (!incident.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return incident;
    }
}
