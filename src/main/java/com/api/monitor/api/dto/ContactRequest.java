package com.api.monitor.api.dto;

public record ContactRequest(
        String name,
        String email,
        String subject,
        String message
) {}
