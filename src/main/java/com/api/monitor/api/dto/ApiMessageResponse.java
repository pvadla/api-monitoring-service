package com.api.monitor.api.dto;

import java.util.Map;

public record ApiMessageResponse(String error) {
    public static Map<String, String> error(String msg) {
        return Map.of("error", msg);
    }

    public static Map<String, Boolean> success() {
        return Map.of("success", true);
    }
}
