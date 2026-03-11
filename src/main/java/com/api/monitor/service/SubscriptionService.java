package com.api.monitor.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.api.monitor.entity.Subscription;
import com.api.monitor.entity.User;
import com.api.monitor.repository.SubscriptionRepository;
import com.api.monitor.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates subscription lifecycle: create checkout, apply webhook events, cancel/upgrade/downgrade.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final RazorpayService razorpayService;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    /** Total billing cycles for new subscriptions; 0 = infinite. */
    private static final int SUBSCRIPTION_TOTAL_COUNT = 0;

    /**
     * Create subscription checkout: ensure customer exists, create Razorpay subscription, persist Subscription.
     * Returns subscription id and key for frontend Checkout.
     */
    @Transactional
    public SubscriptionCheckoutResult createSubscriptionCheckout(User user, String planSlug) {
        String customerId = getOrCreateRazorpayCustomerId(user);
        String planId = razorpayService.getPlanIdForSlug(planSlug);
        String subscriptionId = razorpayService.createSubscription(planId, customerId, SUBSCRIPTION_TOTAL_COUNT);

        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setRazorpaySubscriptionId(subscriptionId);
        sub.setRazorpayPlanId(planId);
        sub.setPlanSlug(planSlug.toUpperCase());
        sub.setStatus("created");
        sub.setCreatedAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);

        return new SubscriptionCheckoutResult(subscriptionId, razorpayService.getKeyId(), planSlug);
    }

    private String getOrCreateRazorpayCustomerId(User user) {
        if (user.getRazorpayCustomerId() != null && !user.getRazorpayCustomerId().isBlank()) {
            return user.getRazorpayCustomerId();
        }
        String customerId = razorpayService.createCustomer(user.getName(), user.getEmail());
        user.setRazorpayCustomerId(customerId);
        userRepository.save(user);
        return customerId;
    }

    /**
     * Verify subscription payment (from frontend success handler) and activate.
     */
    @Transactional
    public boolean verifyAndActivateSubscription(String paymentId, String subscriptionId, String signature, User user) {
        if (!razorpayService.verifySubscriptionPaymentSignature(paymentId, subscriptionId, signature)) {
            return false;
        }
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(subscriptionId).orElse(null);
        if (sub == null || !sub.getUser().getId().equals(user.getId())) {
            return false;
        }
        sub.setStatus("active");
        sub.touch();
        subscriptionRepository.save(sub);
        user.setSubscriptionTier(sub.getPlanSlug());
        userRepository.save(user);
        return true;
    }

    // ─── Webhook handlers ────────────────────────────────────────────────────

    @Transactional
    public void onSubscriptionActivated(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        String status = (String) subEntity.get("status");
        String planId = (String) subEntity.get("plan_id");

        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) {
            log.info("Webhook subscription.activated: no local subscription for {}", razorpaySubId);
            return;
        }
        sub.setStatus(status != null ? status : "active");
        sub.setRazorpayPlanId(planId);
        setPeriodDates(sub, subEntity);
        sub.touch();
        subscriptionRepository.save(sub);

        User user = sub.getUser();
        user.setSubscriptionTier(sub.getPlanSlug());
        userRepository.save(user);
        log.info("Subscription activated: user={} plan={}", user.getEmail(), sub.getPlanSlug());
    }

    @Transactional
    public void onSubscriptionCharged(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) return;
        setPeriodDates(sub, subEntity);
        sub.touch();
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void onSubscriptionCancelled(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) return;
        sub.setStatus("cancelled");
        sub.setCancelledAt(LocalDateTime.now());
        setPeriodDates(sub, subEntity);
        sub.touch();
        subscriptionRepository.save(sub);
        downgradeUserIfNoActiveSubscription(sub.getUser());
        log.info("Subscription cancelled: {}", razorpaySubId);
    }

    @Transactional
    public void onSubscriptionCompleted(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) return;
        sub.setStatus("completed");
        sub.touch();
        subscriptionRepository.save(sub);
        downgradeUserIfNoActiveSubscription(sub.getUser());
    }

    @Transactional
    public void onSubscriptionPending(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) return;
        sub.setStatus("pending");
        setPeriodDates(sub, subEntity);
        sub.touch();
        subscriptionRepository.save(sub);
    }

    @Transactional
    public void onSubscriptionAuthenticated(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> subEntity = (Map<String, Object>) payload.get("subscription");
        if (subEntity == null) return;
        String razorpaySubId = (String) subEntity.get("id");
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubId).orElse(null);
        if (sub == null) return;
        sub.setStatus("authenticated");
        setPeriodDates(sub, subEntity);
        sub.touch();
        subscriptionRepository.save(sub);
    }

    private void setPeriodDates(Subscription sub, Map<String, Object> subEntity) {
        Object start = subEntity.get("current_start");
        Object end = subEntity.get("current_end");
        if (start instanceof Number) {
            sub.setCurrentStart(LocalDateTime.ofInstant(Instant.ofEpochSecond(((Number) start).longValue()), ZoneId.systemDefault()));
        }
        if (end instanceof Number) {
            sub.setCurrentEnd(LocalDateTime.ofInstant(Instant.ofEpochSecond(((Number) end).longValue()), ZoneId.systemDefault()));
        }
    }

    private void downgradeUserIfNoActiveSubscription(User user) {
        List<Subscription> active = subscriptionRepository.findByUserAndStatusIn(user, List.of("active", "authenticated"));
        if (active.isEmpty()) {
            user.setSubscriptionTier("FREE");
            userRepository.save(user);
            log.info("User downgraded to FREE: {}", user.getEmail());
        }
    }

    // ─── Lifecycle: cancel, upgrade, downgrade ─────────────────────────────────

    @Transactional
    public void cancelSubscription(User user, String razorpaySubscriptionId, boolean atPeriodEnd) {
        Subscription sub = subscriptionRepository.findByRazorpaySubscriptionId(razorpaySubscriptionId).orElse(null);
        if (sub == null || !sub.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Subscription not found or not owned by user");
        }
        razorpayService.cancelSubscription(razorpaySubscriptionId, atPeriodEnd);
        sub.setCancelAtPeriodEnd(atPeriodEnd);
        sub.touch();
        subscriptionRepository.save(sub);
        if (!atPeriodEnd) {
            sub.setStatus("cancelled");
            sub.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(sub);
            downgradeUserIfNoActiveSubscription(user);
        }
    }

    /** Upgrade/downgrade: cancel current subscription (at period end) and return new plan slug to subscribe. */
    @Transactional
    public String schedulePlanChange(User user, String newPlanSlug) {
        List<Subscription> active = subscriptionRepository.findByUserAndStatusIn(user, List.of("active", "authenticated"));
        for (Subscription sub : active) {
            razorpayService.cancelSubscription(sub.getRazorpaySubscriptionId(), true);
            sub.setCancelAtPeriodEnd(true);
            sub.touch();
            subscriptionRepository.save(sub);
        }
        return newPlanSlug;
    }

    public static final class SubscriptionCheckoutResult {
        private final String subscriptionId;
        private final String keyId;
        private final String planSlug;

        public SubscriptionCheckoutResult(String subscriptionId, String keyId, String planSlug) {
            this.subscriptionId = subscriptionId;
            this.keyId = keyId;
            this.planSlug = planSlug;
        }
        public String getSubscriptionId() { return subscriptionId; }
        public String getKeyId() { return keyId; }
        public String getPlanSlug() { return planSlug; }
    }
}
