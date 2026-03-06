package com.api.monitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.api.monitor.service.EmailNotificationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ContactController {

    private final EmailNotificationService emailNotificationService;

    @GetMapping("/contact")
    public String form(Model model) {
        return "contact";
    }

    @PostMapping("/contact")
    public String submit(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String subject,
            @RequestParam String message,
            RedirectAttributes redirectAttributes) {

        emailNotificationService.sendContactEmail(name, email, subject, message);
        redirectAttributes.addFlashAttribute("success", "Thanks for reaching out! We'll get back to you soon.");
        return "redirect:/contact";
    }
}

