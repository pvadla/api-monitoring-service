package com.api.monitor.api.dto;

public record CreateEndpointRequest(
        String name,
        String url,
        Integer checkInterval,
        String expectedBodySubstring
) {}
