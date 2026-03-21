package com.api.monitor.api.dto;

import com.api.monitor.entity.EndpointCheck;

public record EndpointCheckResponse(
        Long id,
        String checkedAt,
        Long responseTimeMs,
        Integer statusCode,
        Boolean isUp,
        String errorMessage
) {
    public static EndpointCheckResponse fromEntity(EndpointCheck c) {
        return new EndpointCheckResponse(
                c.getId(),
                c.getCheckedAt() == null ? null : c.getCheckedAt().toString(),
                c.getResponseTimeMs(),
                c.getStatusCode(),
                c.getIsUp(),
                c.getErrorMessage()
        );
    }
}
