package com.api.monitor.api.dto;

import java.util.List;



/**
 * {@code GET /api/dashboard} — mirrors Thymeleaf dashboard model attributes.
 */
public record DashboardResponse(
        UserResponse user,
        long endpointCount,
        long upCount,
        long downCount,
        List<EndpointResponse> endpoints,
        List<HeartbeatMonitorResponse> heartbeats,
        List<SslMonitorResponse> sslMonitors,
        String baseUrl,
        String flashSuccess,
        /** Total open (unresolved) incidents for this user. */
        long openIncidentCount
) {}
