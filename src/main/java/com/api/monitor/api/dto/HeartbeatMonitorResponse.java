package com.api.monitor.api.dto;

import java.time.LocalDateTime;

import com.api.monitor.entity.HeartbeatMonitor;

public record HeartbeatMonitorResponse(
        Long id,
        String name,
        String token,
        Integer expectedIntervalMinutes,
        String lastPingAt,
        Boolean isActive
) {
    public static HeartbeatMonitorResponse fromEntity(HeartbeatMonitor h) {
        return new HeartbeatMonitorResponse(
                h.getId(),
                h.getName(),
                h.getToken(),
                h.getExpectedIntervalMinutes(),
                format(h.getLastPingAt()),
                h.getIsActive()
        );
    }

    private static String format(LocalDateTime t) {
        return t == null ? null : t.toString();
    }
}
