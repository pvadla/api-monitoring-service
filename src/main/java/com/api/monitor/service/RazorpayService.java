package com.api.monitor.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Razorpay API: orders (one-time), subscriptions (plans, customers, subscriptions), webhook verification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private static final String RAZORPAY_BASE = "https://api.razorpay.com/v1";

    /**
     * WebClient for calling Razorpay. We build it locally so this service does not
     * depend on any external WebClient bean in environments where it might not exist.
     */
    private final WebClient webClient = WebClient.builder()
            .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(512 * 1024))
            .build();

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @Value("${razorpay.starter-amount-paise:58000}")
    private long starterAmountPaise;

    @Value("${razorpay.plan.starter:}")
    private String starterPlanId;

    @Value("${razorpay.plan.pro:}")
    private String proPlanId;

    private String authHeader() {
        return "Basic " + Base64.getEncoder().encodeToString((keyId + ":" + keySecret).getBytes(StandardCharsets.UTF_8));
    }

    private void requireKeys() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay key-id and key-secret must be configured.");
        }
    }

    // ─── One-time orders (legacy) ─────────────────────────────────────────────
    public CreateOrderResult createOrder(String plan, String receiptId) {
        requireKeys();
        long amount = "STARTER".equalsIgnoreCase(plan) ? starterAmountPaise : starterAmountPaise;
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("currency", "INR");
        body.put("receipt", receiptId);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = post("/orders", body);
            if (res == null) throw new RuntimeException("Razorpay create order returned null");
            String orderId = (String) res.get("id");
            Object amt = res.get("amount");
            long amountPaise = amt instanceof Number ? ((Number) amt).longValue() : Long.parseLong(String.valueOf(amt));
            return new CreateOrderResult(orderId, amountPaise, keyId);
        } catch (WebClientResponseException e) {
            log.warn("Razorpay create order failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Payment gateway error: " + e.getResponseBodyAsString(), e);
        }
    }

    public boolean verifyPayment(String paymentId, String orderId) {
        requireKeys();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payment = get("/payments/" + paymentId);
            if (payment == null) return false;
            if (!"captured".equalsIgnoreCase((String) payment.get("status"))) return false;
            Object orderIdInPayment = payment.get("order_id");
            String expectedOrderId = orderIdInPayment instanceof String ? (String) orderIdInPayment : null;
            return orderId != null && orderId.equals(expectedOrderId);
        } catch (WebClientResponseException e) {
            log.warn("Razorpay verify payment failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        }
    }

    // ─── Customers ───────────────────────────────────────────────────────────
    /** Create a Razorpay customer. Returns customer id (e.g. cust_xxx). */
    public String createCustomer(String name, String email) {
        requireKeys();
        Map<String, Object> body = new HashMap<>();
        body.put("name", name != null ? name : "Customer");
        body.put("email", email != null ? email : "");
        @SuppressWarnings("unchecked")
        Map<String, Object> res = post("/customers", body);
        if (res == null) throw new RuntimeException("Razorpay create customer returned null");
        return (String) res.get("id");
    }

    // ─── Plans (use configured plan IDs; Razorpay plan creation can be done in Dashboard) ───
    public String getPlanIdForSlug(String planSlug) {
        if ("STARTER".equalsIgnoreCase(planSlug) && starterPlanId != null && !starterPlanId.isBlank()) {
            return starterPlanId;
        }
        if ("PRO".equalsIgnoreCase(planSlug) && proPlanId != null && !proPlanId.isBlank()) {
            return proPlanId;
        }
        throw new IllegalStateException("Razorpay plan not configured for: " + planSlug + ". Set razorpay.plan.starter / razorpay.plan.pro.");
    }

    // ─── Subscriptions ───────────────────────────────────────────────────────
    /** Create a subscription. Returns subscription id (e.g. sub_xxx). */
    public String createSubscription(String planId, String customerId, int totalCount) {
        requireKeys();
        Map<String, Object> body = new HashMap<>();
        body.put("plan_id", planId);
        body.put("customer_id", customerId);
        body.put("total_count", totalCount); // 0 = infinite, or e.g. 12 for 12 months
        @SuppressWarnings("unchecked")
        Map<String, Object> res = post("/subscriptions", body);
        if (res == null) throw new RuntimeException("Razorpay create subscription returned null");
        return (String) res.get("id");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchSubscription(String subscriptionId) {
        requireKeys();
        return get("/subscriptions/" + subscriptionId);
    }

    /** Cancel subscription. cancelAtCycleEnd=true cancels at period end, false cancels immediately. */
    public void cancelSubscription(String subscriptionId, boolean cancelAtCycleEnd) {
        requireKeys();
        Map<String, Object> body = new HashMap<>();
        body.put("cancel_at_cycle_end", cancelAtCycleEnd);
        post("/subscriptions/" + subscriptionId + "/cancel", body);
    }

    /** Verify subscription payment signature (from Checkout success handler). */
    public boolean verifySubscriptionPaymentSignature(String paymentId, String subscriptionId, String signature) {
        if (keySecret == null || keySecret.isBlank()) return false;
        String payload = paymentId + "|" + subscriptionId;
        String expected = hmacSha256(keySecret, payload);
        return expected != null && expected.equals(signature);
    }

    // ─── Webhooks ────────────────────────────────────────────────────────────
    /** Verify X-Razorpay-Signature using webhook secret and raw body. */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Razorpay webhook secret not set; skipping verification");
            return false;
        }
        String expected = hmacSha256(webhookSecret, rawBody);
        return expected != null && expected.equals(signature);
    }

    private static String hmacSha256(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> get(String path) {
        return webClient.get()
            .uri(RAZORPAY_BASE + path)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        return webClient.post()
            .uri(RAZORPAY_BASE + path)
            .header(HttpHeaders.AUTHORIZATION, authHeader())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(Map.class)
            .block();
    }

    public long getStarterAmountPaise() { return starterAmountPaise; }
    public String getKeyId() { return keyId; }

    public static final class CreateOrderResult {
        private final String orderId;
        private final long amountPaise;
        private final String keyId;

        public CreateOrderResult(String orderId, long amountPaise, String keyId) {
            this.orderId = orderId;
            this.amountPaise = amountPaise;
            this.keyId = keyId;
        }
        public String getOrderId() { return orderId; }
        public long getAmountPaise() { return amountPaise; }
        public String getKeyId() { return keyId; }
    }
}
