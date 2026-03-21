package com.api.monitor.api.dto;

import com.api.monitor.entity.User;

/**
 * Safe subset of {@link User} for JSON API (Phase 0 contract / Phase 2a {@code GET /api/me}).
 */
public record UserResponse(
        Long id,
        String email,
        String name,
        String picture,
        String subscriptionTier,
        String statusSlug,
        String statusPageTitle,
        String statusPageLogoUrl,
        Boolean notifyOnEndpointDown,
        Boolean notifyOnEndpointRecovery
) {
    public static UserResponse fromEntity(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getName(),
                u.getPicture(),
                u.getSubscriptionTier() != null ? u.getSubscriptionTier() : "FREE",
                u.getStatusSlug(),
                u.getStatusPageTitle(),
                u.getStatusPageLogoUrl(),
                u.getNotifyOnEndpointDown(),
                u.getNotifyOnEndpointRecovery()
        );
    }
}
