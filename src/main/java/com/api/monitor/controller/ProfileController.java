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
@RequestMapping("/settings/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    public String form(@AuthenticationPrincipal OAuth2User principal, Model model) {
        User user = getUser(principal);
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping
    public String save(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean notifyOnEndpointDown,
            @RequestParam(required = false) Boolean notifyOnEndpointRecovery,
            RedirectAttributes redirectAttributes) {

        User user = getUser(principal);
        if (name != null) {
            user.setName(name.trim());
        }
        user.setNotifyOnEndpointDown(Boolean.TRUE.equals(notifyOnEndpointDown));
        user.setNotifyOnEndpointRecovery(Boolean.TRUE.equals(notifyOnEndpointRecovery));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Profile saved.");
        return "redirect:/settings/profile";
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
