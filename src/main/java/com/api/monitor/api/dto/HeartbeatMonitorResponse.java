package com.api.monitor.api.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.api.monitor.entity.HeartbeatCheck;
import com.api.monitor.entity.HeartbeatMonitor;

public record HeartbeatMonitorResponse(
        Long id,
        String name,
        String token,
        Integer expectedIntervalMinutes,
        String lastPingAt,
        Boolean isActive,
        /** null = pending (never pinged and not yet overdue), true = up, false = down/missed */
        Boolean isUp,
        /** Last 15 scheduler evaluations, oldest → newest. null = slot not filled yet. */
        List<Boolean> recentChecksUp
) {
    public static HeartbeatMonitorResponse fromEntity(HeartbeatMonitor h) {
        return new HeartbeatMonitorResponse(
                h.getId(),
                h.getName(),
                h.getToken(),
                h.getExpectedIntervalMinutes(),
                format(h.getLastPingAt()),
                h.getIsActive(),
                h.getIsUp(),
                emptyRecentChecks()
        );
    }

    public static HeartbeatMonitorResponse fromEntity(HeartbeatMonitor h, List<Boolean> recentChecksUp) {
        return new HeartbeatMonitorResponse(
                h.getId(),
                h.getName(),
                h.getToken(),
                h.getExpectedIntervalMinutes(),
                format(h.getLastPingAt()),
                h.getIsActive(),
                h.getIsUp(),
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
     * @param topDescNewestFirst rows from {@code findTop15ByHeartbeatMonitorOrderByCheckedAtDesc} (newest first)
     */
    public static List<Boolean> recentChecksUpFromRows(List<HeartbeatCheck> topDescNewestFirst) {
        if (topDescNewestFirst == null || topDescNewestFirst.isEmpty()) {
            return emptyRecentChecks();
        }
        int n = Math.min(15, topDescNewestFirst.size());
        List<HeartbeatCheck> newest15 = topDescNewestFirst.subList(0, n);
        List<HeartbeatCheck> chronological = new ArrayList<>(newest15);
        Collections.reverse(chronological);
        List<Boolean> out = new ArrayList<>(15);
        int pad = 15 - chronological.size();
        for (int i = 0; i < pad; i++) {
            out.add(null);
        }
        for (HeartbeatCheck c : chronological) {
            out.add(Boolean.TRUE.equals(c.getIsUp()));
        }
        return out;
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
