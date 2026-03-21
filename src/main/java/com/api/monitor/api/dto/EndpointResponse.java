package com.api.monitor.api.dto;

import java.time.LocalDateTime;

import com.api.monitor.entity.Endpoint;

/**
 * JSON view of an {@link Endpoint} (no user relation).
 */
public record EndpointResponse(
        Long id,
        String name,
        String url,
        Integer checkInterval,
        Boolean isActive,
        Boolean isUp,
        Integer failureCount,
        String lastChecked,
        Boolean showOnStatusPage,
        String expectedBodySubstring,
        String sslExpiresAt
) {
    public static EndpointResponse fromEntity(Endpoint e) {
        return new EndpointResponse(
                e.getId(),
                e.getName(),
                e.getUrl(),
                e.getCheckInterval(),
                e.getIsActive(),
                e.getIsUp(),
                e.getFailureCount(),
                format(e.getLastChecked()),
                e.getShowOnStatusPage(),
                e.getExpectedBodySubstring(),
                format(e.getSslExpiresAt())
        );
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
