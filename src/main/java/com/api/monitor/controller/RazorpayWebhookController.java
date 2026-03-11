package com.api.monitor.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.service.RazorpayService;
import com.api.monitor.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Receives Razorpay webhooks. Must verify X-Razorpay-Signature with raw body.
 */
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
@Slf4j
public class RazorpayWebhookController {

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            Jackson2ObjectMapperBuilder.json().build();

    private final RazorpayService razorpayService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/razorpay")
    public ResponseEntity<Void> handle(@RequestBody String rawBody,
                                      @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook: missing X-Razorpay-Signature");
            return ResponseEntity.badRequest().build();
        }
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            log.warn("Razorpay webhook: invalid signature");
            return ResponseEntity.badRequest().build();
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = JSON.readValue(rawBody, Map.class);
            String event = (String) payload.get("event");
            if (event == null) {
                return ResponseEntity.ok().build();
            }

            switch (event) {
                case "subscription.activated" -> subscriptionService.onSubscriptionActivated(payload);
                case "subscription.authenticated" -> subscriptionService.onSubscriptionAuthenticated(payload);
                case "subscription.charged" -> subscriptionService.onSubscriptionCharged(payload);
                case "subscription.cancelled" -> subscriptionService.onSubscriptionCancelled(payload);
                case "subscription.completed" -> subscriptionService.onSubscriptionCompleted(payload);
                case "subscription.pending" -> subscriptionService.onSubscriptionPending(payload);
                default -> log.debug("Razorpay webhook: unhandled event {}", event);
            }
        } catch (Exception e) {
            log.error("Razorpay webhook: failed to process", e);
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }
}
