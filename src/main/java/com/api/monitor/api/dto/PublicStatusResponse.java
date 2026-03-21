package com.api.monitor.api.dto;

import java.util.List;

/**
 * {@code GET /api/public/status/{slug}}
 */
public record PublicStatusResponse(
        String slug,
        String pageTitle,
        String logoUrl,
        String overallStatusLabel,
        String statusKind,
        List<EndpointResponse> endpoints,
        List<IncidentResponse> incidents
) {}
