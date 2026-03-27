package com.api.monitor.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.api.monitor.entity.SslCheck;
import com.api.monitor.entity.SslMonitor;

public record SslMonitorResponse(
        Long id,
        String name,
        String domain,
        int port,
        int alertDaysThreshold,
        Boolean isActive,
        /**
         * null  = never checked.
         * true  = cert healthy (daysLeft > alertDaysThreshold).
         * false = cert expired, TLS error, or daysLeft <= alertDaysThreshold.
         */
        Boolean isUp,
        /** ISO string of cert expiry date, or null if never checked successfully. */
        String sslExpiresAt,
        /** ISO string of last check time, or null if never checked. */
        String lastCheckedAt,
        /** Last 15 checks, oldest → newest. null slot = not filled yet. */
        List<Boolean> recentChecksUp
) {

    public static SslMonitorResponse fromEntity(SslMonitor m) {
        return new SslMonitorResponse(
                m.getId(), m.getName(), m.getDomain(), m.getPort(),
                m.getAlertDaysThreshold(), m.getIsActive(), m.getIsUp(),
                format(m.getSslExpiresAt()), format(m.getLastCheckedAt()),
                emptyRecentChecks()
        );
    }

    public static SslMonitorResponse fromEntity(SslMonitor m, List<Boolean> recentChecksUp) {
        return new SslMonitorResponse(
                m.getId(), m.getName(), m.getDomain(), m.getPort(),
                m.getAlertDaysThreshold(), m.getIsActive(), m.getIsUp(),
                format(m.getSslExpiresAt()), format(m.getLastCheckedAt()),
                recentChecksUp
        );
    }

    /**
     * @param topDescNewestFirst rows from {@code findTop15BySslMonitorOrderByCheckedAtDesc} (newest first)
     */
    public static List<Boolean> recentChecksUpFromRows(List<SslCheck> topDescNewestFirst) {
        if (topDescNewestFirst == null || topDescNewestFirst.isEmpty()) {
            return emptyRecentChecks();
        }
        int n = Math.min(15, topDescNewestFirst.size());
        List<SslCheck> newest15 = topDescNewestFirst.subList(0, n);
        List<SslCheck> chronological = new ArrayList<>(newest15);
        Collections.reverse(chronological);
        List<Boolean> out = new ArrayList<>(15);
        int pad = 15 - chronological.size();
        for (int i = 0; i < pad; i++) {
            out.add(null);
        }
        for (SslCheck c : chronological) {
            out.add(Boolean.TRUE.equals(c.getIsUp()));
        }
        return out;
    }

    private static List<Boolean> emptyRecentChecks() {
        List<Boolean> list = new ArrayList<>(15);
        for (int i = 0; i < 15; i++) {
            list.add(null);
        }
        return list;
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
