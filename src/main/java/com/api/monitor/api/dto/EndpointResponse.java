package com.api.monitor.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;

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
        String sslExpiresAt,
        /** Last 15 checks, oldest → newest (left → right). {@code null} = no check in that slot yet. */
        List<Boolean> recentChecksUp
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
                format(e.getSslExpiresAt()),
                emptyRecentChecks()
        );
    }

    public static EndpointResponse fromEntity(Endpoint e, List<Boolean> recentChecksUp) {
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
                format(e.getSslExpiresAt()),
                recentChecksUp
        );
    }

    private static List<Boolean> emptyRecentChecks() {
        List<Boolean> list = new ArrayList<>(15);
        for (int i = 0; i < 15; i++) {
            list.add(null);
        }
        return list;
    }

    /**
     * @param topDescNewestFirst rows from {@code findTop15ByEndpointOrderByCheckedAtDesc} (newest first)
     */
    public static List<Boolean> recentChecksUpFromRows(List<EndpointCheck> topDescNewestFirst) {
        if (topDescNewestFirst == null || topDescNewestFirst.isEmpty()) {
            return emptyRecentChecks();
        }
        int n = Math.min(15, topDescNewestFirst.size());
        List<EndpointCheck> newest15 = topDescNewestFirst.subList(0, n);
        List<EndpointCheck> chronological = new ArrayList<>(newest15);
        Collections.reverse(chronological);
        List<Boolean> out = new ArrayList<>(15);
        int pad = 15 - chronological.size();
        for (int i = 0; i < pad; i++) {
            out.add(null);
        }
        for (EndpointCheck c : chronological) {
            out.add(Boolean.TRUE.equals(c.getIsUp()));
        }
        return out;
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
