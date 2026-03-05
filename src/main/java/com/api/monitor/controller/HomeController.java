package com.api.monitor.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;

    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(value = "accountDeleted", required = false) Boolean accountDeleted,
            Model model) {
        if (principal != null) {
            String displayName = principal.getAttribute("name");
            model.addAttribute("userDisplayName", displayName != null ? displayName : "User");
        }
        if (Boolean.TRUE.equals(accountDeleted)) {
            model.addAttribute("success", "Your account and all monitoring data have been deleted. You can sign in again with Google anytime.");
        }
        return "index";  // loads index.html
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OAuth2User principal, Model model) {
        String email = principal.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseThrow();

        long total = endpointRepository.countByUser(user);
        long upCount = endpointRepository.countByUserAndIsUp(user, true);
        long downCount = endpointRepository.countByUserAndIsUp(user, false);

        List<Endpoint> endpoints = endpointRepository.findByUser(user);

        model.addAttribute("user", user);
        model.addAttribute("endpointCount", total);
        model.addAttribute("upCount", upCount);
        model.addAttribute("downCount", downCount);
        model.addAttribute("endpoints", endpoints);

        return "dashboard";  // loads dashboard.html
    }

    @GetMapping("/debug-env")
    @ResponseBody
    public String debugEnv() {
        StringBuilder sb = new StringBuilder("<pre>");
        var env = System.getenv();
        env.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> {
                    String key = e.getKey();
                    String value = e.getValue();
                    boolean sensitive = key.matches("(?i).*(PASSWORD|SECRET|KEY|TOKEN|CREDENTIAL|DATABASE_URL).*");
                    sb.append(key).append("=").append(sensitive ? "[REDACTED]" : value).append("\n");
                });
        return sb.append("</pre>").toString();
    }

    @GetMapping("/debug-env1")
    @ResponseBody
    public String debugEnv1(
            @Value("${GOOGLE_CLIENT_ID:NOT_FOUND}") String googleId,
            @Value("${GOOGLE_CLIENT_SECRET:NOT_FOUND}") String googleSecret,
            @Value("${SPRING_PROFILES_ACTIVE:NOT_FOUND}") String profile,
            @Value("${spring.security.oauth2.client.registration.google.client-id:NOT_FOUND}") String resolvedId
    ) {
        return "<pre>"
                + "=== System.getenv() ==="
                + "\nGOOGLE_CLIENT_ID (env):     " + System.getenv("GOOGLE_CLIENT_ID")
                + "\nGOOGLE_CLIENT_SECRET (env): " + System.getenv("GOOGLE_CLIENT_SECRET")
                + "\nSPRING_PROFILES_ACTIVE:     " + System.getenv("SPRING_PROFILES_ACTIVE")
                + "\n\n=== @Value injection ==="
                + "\nGOOGLE_CLIENT_ID:     " + googleId
                + "\nGOOGLE_CLIENT_SECRET: " + googleSecret
                + "\nPROFILE:              " + profile
                + "\n\n=== Resolved OAuth value ==="
                + "\nclient-id resolved:   " + resolvedId
                + "\n\n=== Active Profiles ==="
                + "\nProfiles: " + System.getProperty("spring.profiles.active")
                + "</pre>";
    }
}
