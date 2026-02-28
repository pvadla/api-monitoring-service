package com.api.monitor.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.monitor.entity.User;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/settings/status")
@RequiredArgsConstructor
public class StatusSettingsController {

    private final UserRepository userRepository;

    @GetMapping
    public String form(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = getUser(principal);
        model.addAttribute("user", user);
        model.addAttribute("statusSlug", user.getStatusSlug());
        model.addAttribute("statusPageTitle", user.getStatusPageTitle());
        model.addAttribute("statusPageLogoUrl", user.getStatusPageLogoUrl());
        return "status-settings";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String statusSlug,
            @RequestParam(required = false) String statusPageTitle,
            @RequestParam(required = false) String statusPageLogoUrl,
            RedirectAttributes redirectAttributes) {

        User user = getUser(principal);

        if (statusSlug != null && !statusSlug.isBlank()) {
            statusSlug = statusSlug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-").replaceAll("-+", "-");
            if (!statusSlug.isBlank()) {
                user.setStatusSlug(statusSlug);
            }
        }
        user.setStatusPageTitle(statusPageTitle != null && !statusPageTitle.isBlank() ? statusPageTitle.trim() : null);
        user.setStatusPageLogoUrl(statusPageLogoUrl != null && !statusPageLogoUrl.isBlank() ? statusPageLogoUrl.trim() : null);

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Status page settings saved.");
        return "redirect:/settings/status";
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
