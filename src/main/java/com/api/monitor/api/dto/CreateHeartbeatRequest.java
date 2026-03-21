package com.api.monitor.api.dto;

public record CreateHeartbeatRequest(
        String name,
        Integer expectedIntervalMinutes
) {}
