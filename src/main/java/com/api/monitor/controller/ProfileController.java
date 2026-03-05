package com.api.monitor.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/settings/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final IncidentRepository incidentRepository;

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

    @PostMapping("/delete-account")
    @Transactional
    public String deleteAccount(
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = getUser(principal);

        // Load all endpoints for this user
        List<Endpoint> endpoints = endpointRepository.findByUser(user);

        if (!endpoints.isEmpty()) {
            // Remove all checks for these endpoints
            endpointCheckRepository.deleteByEndpointIn(endpoints);
        }

        // Remove all incidents for this user
        incidentRepository.deleteByUser(user);

        // Remove endpoints themselves
        if (!endpoints.isEmpty()) {
            endpointRepository.deleteAll(endpoints);
        }

        // Finally, delete the user record
        userRepository.delete(user);

        // Log out: clear security context and invalidate session so home page shows "Sign in with Google"
        new SecurityContextLogoutHandler().logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/?accountDeleted=1";
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
