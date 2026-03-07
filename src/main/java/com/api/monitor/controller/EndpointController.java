package com.api.monitor.controller;

import java.util.Collections;
import java.util.List;

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

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;
    private final UserRepository userRepository;

    // ─── Add ────────────────────────────────────────────────────────────────
    @PostMapping("/add")
    public String addEndpoint(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam Integer checkInterval,
            @RequestParam(required = false) String expectedBodySubstring,
            RedirectAttributes redirectAttributes) {

        User user = getUser(principal);

        // Enforce FREE tier limit: max 5 monitors total (endpoints + heartbeats)
        String tier = user.getSubscriptionTier();
        if (tier == null || tier.equalsIgnoreCase("FREE")) {
            long endpoints = endpointRepository.countByUser(user);
            long heartbeats = heartbeatMonitorRepository.findByUser(user).size();
            if (endpoints + heartbeats >= 5) {
                redirectAttributes.addFlashAttribute("error",
                        "Free plan limit reached: you can have up to 5 monitors (HTTP + heartbeat). Remove one or upgrade your plan.");
                return "redirect:/dashboard";
            }
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setUser(user);
        endpoint.setName(name);
        endpoint.setUrl(url);
        endpoint.setCheckInterval(checkInterval);
        endpoint.setExpectedBodySubstring(
                expectedBodySubstring != null && !expectedBodySubstring.isBlank()
                        ? expectedBodySubstring.trim()
                        : null);

        endpointRepository.save(endpoint);

        redirectAttributes.addFlashAttribute("success", "Endpoint added successfully!");
        return "redirect:/dashboard";
    }

    // ─── Detail page ────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detail(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            Model model) {

        Endpoint endpoint = getOwnedEndpoint(id, principal);

        // Last 50 checks, oldest-first so chart renders left→right
        List<EndpointCheck> checks =
                endpointCheckRepository.findTop50ByEndpointOrderByCheckedAtDesc(endpoint);
        Collections.reverse(checks);

        // Uptime percentage
        long total   = endpointCheckRepository.countByEndpoint(endpoint);
        long upCount = endpointCheckRepository.countByEndpointAndIsUp(endpoint, true);
        double uptimePct = total == 0 ? 100.0 : (upCount * 100.0 / total);

        // Average response time (exclude failed checks)
        double avgResponseMs = checks.stream()
        .filter(c -> c.getResponseTimeMs() != null)
        .mapToLong(c -> c.getResponseTimeMs())
        .average()
        .orElse(0.0);

        User user = userRepository.findByEmail(principal.getAttribute("email")).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute("endpoint", endpoint);
        model.addAttribute("checks", checks);
        model.addAttribute("uptimePct", String.format("%.2f", uptimePct));
        model.addAttribute("avgResponseMs", String.format("%.0f", avgResponseMs));
        model.addAttribute("pageTitle", endpoint.getName() + " - APIWatch");
        model.addAttribute("activeNav", "dashboard");

        return "endpoint-detail";
    }

    // ─── Edit (update) ──────────────────────────────────────────────────────
    @PostMapping("/{id}/edit")
    public String edit(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam Integer checkInterval,
            @RequestParam(required = false) String expectedBodySubstring,
            RedirectAttributes redirectAttributes) {

        Endpoint endpoint = getOwnedEndpoint(id, principal);
        endpoint.setName(name.trim());
        endpoint.setUrl(url.trim());
        endpoint.setCheckInterval(checkInterval);
        endpoint.setExpectedBodySubstring(
                expectedBodySubstring != null && !expectedBodySubstring.isBlank()
                        ? expectedBodySubstring.trim()
                        : null);
        endpointRepository.save(endpoint);

        redirectAttributes.addFlashAttribute("success", "Endpoint updated.");
        return "redirect:/dashboard";
    }

    // ─── Delete ─────────────────────────────────────────────────────────────
    @PostMapping("/{id}/delete")
    public String delete(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Endpoint endpoint = getOwnedEndpoint(id, principal);
        endpointRepository.delete(endpoint);

        redirectAttributes.addFlashAttribute("success", "Endpoint deleted.");
        return "redirect:/dashboard";
    }

    // ─── Pause / Resume ─────────────────────────────────────────────────────
    @PostMapping("/{id}/toggle")
    public String toggle(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Endpoint endpoint = getOwnedEndpoint(id, principal);
        endpoint.setIsActive(!endpoint.getIsActive());
        endpointRepository.save(endpoint);

        String msg = endpoint.getIsActive() ? "Monitoring resumed." : "Monitoring paused.";
        redirectAttributes.addFlashAttribute("success", msg);
        return "redirect:/dashboard";
    }

    /** Toggle whether this endpoint is shown on the public status page. */
    @PostMapping("/{id}/toggle-status-visibility")
    public String toggleStatusVisibility(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Endpoint endpoint = getOwnedEndpoint(id, principal);
        boolean currentlyShown = endpoint.getShowOnStatusPage() != null && endpoint.getShowOnStatusPage();
        endpoint.setShowOnStatusPage(!currentlyShown);
        endpointRepository.save(endpoint);

        redirectAttributes.addFlashAttribute("success",
                endpoint.getShowOnStatusPage() ? "Shown on status page." : "Hidden from status page.");
        return "redirect:/dashboard";
    }

    // ─── Helpers ────────────────────────────────────────────────────────────
    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }

    /** Fetch endpoint and verify it belongs to the logged-in user. */
    private Endpoint getOwnedEndpoint(Long id, OAuth2User principal) {
        Endpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endpoint not found"));
        User user = getUser(principal);
        if (!endpoint.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return endpoint;
    }
}