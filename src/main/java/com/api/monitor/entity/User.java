package com.api.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String picture;

    @Column(name = "subscription_tier")
    private String subscriptionTier = "FREE";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Status page (public at /status/{statusSlug})
    @Column(name = "status_slug", unique = true)
    private String statusSlug;

    @Column(name = "status_page_title")
    private String statusPageTitle;

    @Column(name = "status_page_logo_url", length = 1024)
    private String statusPageLogoUrl;

    @Column(name = "notify_on_endpoint_down")
    private Boolean notifyOnEndpointDown = true;

    @Column(name = "notify_on_endpoint_recovery")
    private Boolean notifyOnEndpointRecovery = true;

    /** Razorpay customer id for subscriptions */
    @Column(name = "razorpay_customer_id", length = 64)
    private String razorpayCustomerId;

    // Setters used by CustomOAuth2UserService (explicit for reliability with Lombok)
    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public void setNotifyOnEndpointDown(Boolean notifyOnEndpointDown) {
        this.notifyOnEndpointDown = notifyOnEndpointDown;
    }

    public void setNotifyOnEndpointRecovery(Boolean notifyOnEndpointRecovery) {
        this.notifyOnEndpointRecovery = notifyOnEndpointRecovery;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getSubscriptionTier() { return subscriptionTier; }
    public void setSubscriptionTier(String subscriptionTier) { this.subscriptionTier = subscriptionTier; }
    public String getRazorpayCustomerId() { return razorpayCustomerId; }
    public void setRazorpayCustomerId(String razorpayCustomerId) { this.razorpayCustomerId = razorpayCustomerId; }
}