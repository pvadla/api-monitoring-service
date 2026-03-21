package com.api.monitor.api.dto;

public record UpdateEndpointRequest(
        String name,
        String url,
        Integer checkInterval,
        String expectedBodySubstring
) {}
