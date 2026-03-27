package com.api.monitor.api.dto;

public record CreateSslMonitorRequest(
        String name,
        String domain,
        Integer port,
        Integer alertDaysThreshold
) {}
