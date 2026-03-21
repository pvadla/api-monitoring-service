package com.api.monitor.api.dto;

import java.util.List;

/**
 * {@code GET /api/endpoints/{id}} — mirrors Thymeleaf endpoint detail.
 */
public record EndpointDetailResponse(
        EndpointResponse endpoint,
        List<EndpointCheckResponse> checks,
        String uptimePct,
        String avgResponseMs
) {}
