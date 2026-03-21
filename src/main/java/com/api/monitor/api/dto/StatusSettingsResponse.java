package com.api.monitor.api.dto;

import com.api.monitor.entity.User;

public record StatusSettingsResponse(
        String statusSlug,
        String statusPageTitle,
        String statusPageLogoUrl
) {
    public static StatusSettingsResponse fromEntity(User u) {
        return new StatusSettingsResponse(
                u.getStatusSlug(),
                u.getStatusPageTitle(),
                u.getStatusPageLogoUrl()
        );
    }
}
