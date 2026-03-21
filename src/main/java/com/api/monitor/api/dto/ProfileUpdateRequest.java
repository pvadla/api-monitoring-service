package com.api.monitor.api.dto;

/**
 * {@code PUT /api/settings/profile}
 */
public record ProfileUpdateRequest(
        String name,
        Boolean notifyOnEndpointDown,
        Boolean notifyOnEndpointRecovery
) {}
