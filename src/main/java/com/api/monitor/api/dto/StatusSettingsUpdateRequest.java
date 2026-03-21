package com.api.monitor.api.dto;

/**
 * {@code PUT /api/settings/status}
 */
public record StatusSettingsUpdateRequest(
        String statusSlug,
        String statusPageTitle,
        String statusPageLogoUrl
) {}
