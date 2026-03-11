package com.api.monitor.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.api.monitor.entity.User;
import com.api.monitor.repository.UserRepository;
import com.api.monitor.service.RazorpayService;
import com.api.monitor.service.RazorpayService.CreateOrderResult;
import com.api.monitor.service.SubscriptionService;
import com.api.monitor.service.SubscriptionService.SubscriptionCheckoutResult;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final RazorpayService razorpayService;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    /**
     * Create a Razorpay order for the given plan. Returns orderId, amount (paise), and keyId for frontend checkout.
     */
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String plan) {

        User user = getUser(principal);
        String receiptId = "apw_" + user.getId() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        try {
            CreateOrderResult result = razorpayService.createOrder(plan, receiptId);
            return ResponseEntity.ok(Map.of(
                "orderId", result.getOrderId(),
                "amount", result.getAmountPaise(),
                "keyId", result.getKeyId(),
                "currency", "INR"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create order"));
        }
    }

    /**
     * Verify payment with Razorpay and upgrade user subscription on success.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verify(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> body) {

        String paymentId = body.get("razorpay_payment_id");
        String orderId = body.get("razorpay_order_id");
        String plan = body.get("plan");

        if (paymentId == null || orderId == null || plan == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing payment_id, order_id or plan"));
        }

        User user = getUser(principal);

        if (!razorpayService.verifyPayment(paymentId, orderId)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Payment verification failed"));
        }

        if ("STARTER".equalsIgnoreCase(plan)) {
            user.setSubscriptionTier("STARTER");
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription activated"));
    }

    /**
     * Create subscription checkout (recurring). Returns subscription_id and keyId for Razorpay Checkout with subscription_id.
     */
    @PostMapping("/create-subscription-checkout")
    public ResponseEntity<?> createSubscriptionCheckout(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam String plan) {

        User user = getUser(principal);
        try {
            SubscriptionCheckoutResult result = subscriptionService.createSubscriptionCheckout(user, plan);
            return ResponseEntity.ok(Map.of(
                "subscriptionId", result.getSubscriptionId(),
                "keyId", result.getKeyId(),
                "planSlug", result.getPlanSlug()
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create subscription"));
        }
    }

    /**
     * Verify subscription payment (from Checkout success) and activate plan.
     */
    @PostMapping("/verify-subscription")
    public ResponseEntity<?> verifySubscription(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> body) {

        String paymentId = body.get("razorpay_payment_id");
        String subscriptionId = body.get("razorpay_subscription_id");
        String signature = body.get("razorpay_signature");

        if (paymentId == null || subscriptionId == null || signature == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing payment_id, subscription_id or signature"));
        }

        User user = getUser(principal);
        if (!subscriptionService.verifyAndActivateSubscription(paymentId, subscriptionId, signature, user)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Verification failed"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription activated"));
    }

    private User getUser(OAuth2User principal) {
        String email = principal.getAttribute("email");
        return userRepository.findByEmail(email).orElseThrow();
    }
}
