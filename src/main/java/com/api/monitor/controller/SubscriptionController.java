package com.api.monitor.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.entity.Subscription;
import com.api.monitor.entity.User;
import com.api.monitor.repository.SubscriptionRepository;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    @GetMapping("/current")
    public ResponseEntity<?> current(@AuthenticationPrincipal OAuth2User principal) {
        User user = getUser(principal);
        List<Subscription> subs = subscriptionRepository.findByUserOrderByCreatedAtDesc(user);
        List<Map<String, Object>> list = subs.stream()
            .map(s -> Map.<String, Object>of(
                "id", s.getId(),
                "razorpaySubscriptionId", s.getRazorpaySubscriptionId(),
                "planSlug", s.getPlanSlug(),
                "status", s.getStatus(),
                "currentStart", s.getCurrentStart() != null ? s.getCurrentStart().toString() : "",
                "currentEnd", s.getCurrentEnd() != null ? s.getCurrentEnd().toString() : "",
                "cancelAtPeriodEnd", Boolean.TRUE.equals(s.getCancelAtPeriodEnd())
            ))
            .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("subscriptions", list, "tier", user.getSubscriptionTier() != null ? user.getSubscriptionTier() : "FREE"));
    }

    /**
     * Cancel subscription. Body: { "razorpaySubscriptionId": "sub_xxx", "atPeriodEnd": true }
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancel(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, Object> body) {

        User user = getUser(principal);
        String subId = (String) body.get("razorpaySubscriptionId");
        Boolean atPeriodEnd = body.get("atPeriodEnd") instanceof Boolean b ? b : Boolean.TRUE;

        if (subId == null || subId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing razorpaySubscriptionId"));
        }
        try {
            subscriptionService.cancelSubscription(user, subId, atPeriodEnd);
            return ResponseEntity.ok(Map.of("success", true, "message", atPeriodEnd ? "Subscription will cancel at period end" : "Subscription cancelled"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Schedule plan change (upgrade/downgrade). Cancels current at period end; frontend should then create new subscription for newPlanSlug.
     * Body: { "newPlanSlug": "PRO" } or "STARTER"
     */
    @PostMapping("/change-plan")
    public ResponseEntity<?> changePlan(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> body) {

        User user = getUser(principal);
        String newPlanSlug = body.get("newPlanSlug");
        if (newPlanSlug == null || newPlanSlug.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing newPlanSlug"));
        }
        try {
            subscriptionService.schedulePlanChange(user, newPlanSlug.toUpperCase());
            return ResponseEntity.ok(Map.of("success", true, "newPlanSlug", newPlanSlug.toUpperCase(), "message", "Current subscription will end at period end; you can subscribe to the new plan now."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
