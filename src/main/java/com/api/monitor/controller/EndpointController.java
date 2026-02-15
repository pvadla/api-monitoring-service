package com.api.monitor.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation
    .RequestParam;
import org.springframework.web.servlet.mvc.support
    .RedirectAttributes;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.UserRepository;

import lombok
    .RequiredArgsConstructor;

@Controller
@RequestMapping("/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointRepository endpointRepository;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public String addEndpoint(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String name,
            @RequestParam String url,
            @RequestParam Integer checkInterval,
            RedirectAttributes redirectAttributes) {

        // Get logged in user
        String email = principal.getAttribute("email");
        User user = userRepository
            .findByEmail(email)
            .orElseThrow();

        // Create and save endpoint
        Endpoint endpoint = new Endpoint();
        endpoint.setUser(user);
        endpoint.setName(name);
        endpoint.setUrl(url);
        endpoint.setCheckInterval(checkInterval);

        endpointRepository.save(endpoint);

        // Redirect back to dashboard
        redirectAttributes.addFlashAttribute(
            "success",
            "Endpoint added successfully!"
        );

        return "redirect:/dashboard";
    }
}