package com.api.monitor.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";  // loads index.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
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
