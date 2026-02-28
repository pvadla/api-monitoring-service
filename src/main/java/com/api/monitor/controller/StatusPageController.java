package com.api.monitor.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Public status page per user: /status/{slug}
 * No authentication required.
 */
@Controller
@RequestMapping("/status")
@RequiredArgsConstructor
public class StatusPageController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final IncidentRepository incidentRepository;

    /** Public status page: /status/{slug} */
    @GetMapping("/{slug}")
    public String statusPage(@PathVariable String slug, Model model) {
        User user = userRepository.findByStatusSlug(slug)
                .orElseThrow(() -> new RuntimeException("Status page not found"));

        List<Endpoint> endpoints = endpointRepository.findByUserAndShowOnStatusPageTrue(user);
        List<Incident> incidents = incidentRepository.findLatestByUser(user, 50);

        String title = user.getStatusPageTitle() != null && !user.getStatusPageTitle().isBlank()
                ? user.getStatusPageTitle()
                : "Status";
        String logoUrl = user.getStatusPageLogoUrl();

        long upCount = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getIsUp())).count();
        String overallStatusLabel = endpoints.isEmpty() ? "No endpoints configured"
                : (upCount == endpoints.size() ? "All Systems Operational" : "Some Issues");
        String statusKind = endpoints.isEmpty() ? "none" : (upCount == endpoints.size() ? "all-up" : "issues");

        model.addAttribute("user", user);
        model.addAttribute("slug", slug);
        model.addAttribute("pageTitle", title);
        model.addAttribute("logoUrl", logoUrl);
        model.addAttribute("endpoints", endpoints);
        model.addAttribute("incidents", incidents);
        model.addAttribute("overallStatusLabel", overallStatusLabel);
        model.addAttribute("statusKind", statusKind);

        return "status";
    }

    /** Embed widget: badge for users' websites. Returns HTML snippet. */
    @GetMapping(value = "/{slug}/embed", produces = "text/html")
    @ResponseBody
    public String embed(@PathVariable String slug) {
        Optional<User> userOpt = userRepository.findByStatusSlug(slug);
        if (userOpt.isEmpty()) {
            return "<!-- Status page not found -->";
        }

        User user = userOpt.get();
        List<Endpoint> endpoints = endpointRepository.findByUserAndShowOnStatusPageTrue(user);
        long up = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getIsUp())).count();
        long total = endpoints.size();
        String status = total == 0 ? "unknown" : (up == total ? "operational" : "issues");

        // Build absolute URL for status page (client will need to replace with their domain in production)
        String statusUrl = "/status/" + slug;

        return """
            <div id="apiwatch-badge" data-status="%s" style="display:inline-block;padding:6px 10px;border-radius:6px;font-size:12px;font-family:sans-serif;text-decoration:none;color:inherit;">
              <a href="%s" target="_blank" rel="noopener" style="color:inherit;">
                <span style="font-weight:600;">Status</span>
                <span data-status-label>%s</span>
              </a>
            </div>
            <script>
            (function(){ var el=document.getElementById('apiwatch-badge'); if(!el) return;
            var s=el.getAttribute('data-status'); var label=el.querySelector('[data-status-label]');
            if(s==='operational'){ label.textContent=' All Systems Operational'; el.style.background='#d4edda'; el.style.color='#155724'; }
            else if(s==='issues'){ label.textContent=' Some Issues'; el.style.background='#fff3cd'; el.style.color='#856404'; }
            else{ label.textContent=' No data'; el.style.background='#e2e3e5'; el.style.color='#383d41'; }
            })();
            </script>
            """
            .formatted(status, statusUrl,
                status.equals("operational") ? " All Systems Operational" : status.equals("issues") ? " Some Issues" : " No data");
    }

    /** Embed widget as JS that document.writes the badge (for easy copy-paste). */
    @GetMapping(value = "/{slug}/embed.js", produces = "application/javascript")
    @ResponseBody
    public String embedJs(@PathVariable String slug) {
        Optional<User> userOpt = userRepository.findByStatusSlug(slug);
        if (userOpt.isEmpty()) {
            return "document.write('<!-- Status page not found -->');";
        }

        User user = userOpt.get();
        List<Endpoint> endpoints = endpointRepository.findByUserAndShowOnStatusPageTrue(user);
        long up = endpoints.stream().filter(e -> Boolean.TRUE.equals(e.getIsUp())).count();
        long total = endpoints.size();
        String status = total == 0 ? "unknown" : (up == total ? "operational" : "issues");
        String statusUrl = "/status/" + slug;

        String html = """
            <div id="apiwatch-badge" data-status="%s" style="display:inline-block;padding:6px 10px;border-radius:6px;font-size:12px;font-family:sans-serif;">
              <a href="%s" target="_blank" rel="noopener">Status: %s</a>
            </div>
            """
            .formatted(status, statusUrl,
                status.equals("operational") ? "All Systems Operational" : status.equals("issues") ? "Some Issues" : "No data");

        return "document.write(" + escapeJsString(html) + ");";
    }

    private static String escapeJsString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
}
