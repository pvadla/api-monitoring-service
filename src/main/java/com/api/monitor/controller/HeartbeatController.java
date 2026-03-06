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

import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HeartbeatController {

    private final HeartbeatMonitorRepository heartbeatRepository;
    private final UserRepository userRepository;

    @GetMapping("/heartbeats")
    public String list() {
        return "redirect:/dashboard";
    }

    @PostMapping("/heartbeats")
    public String create(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String name,
            @RequestParam Integer expectedIntervalMinutes) {

        User user = getUser(principal);
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
                .ifPresent(heartbeatRepository::delete);

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
                    hb.setLastNotifiedAt(null); // clear any previous alert
                    heartbeatRepository.save(hb);
                    return "OK";
                })
                .orElse("Unknown heartbeat token");
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}

