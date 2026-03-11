package com.api.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Stores Razorpay subscription for a user. Synced with webhooks and used for lifecycle (upgrade/downgrade/cancel).
 */
@Data
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "razorpay_subscription_id", unique = true, nullable = false, length = 64)
    private String razorpaySubscriptionId;

    @Column(name = "razorpay_plan_id", length = 64)
    private String razorpayPlanId;

    @Column(name = "plan_slug", nullable = false, length = 32)
    private String planSlug; // STARTER, PRO

    @Column(name = "status", nullable = false, length = 32)
    private String status; // created, authenticated, active, cancelled, completed, expired, paused

    @Column(name = "current_start")
    private LocalDateTime currentStart;

    @Column(name = "current_end")
    private LocalDateTime currentEnd;

    @Column(name = "cancel_at_period_end")
    private Boolean cancelAtPeriodEnd = false;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
