package com.example.fastproject.modules.url.event;

public record UrlClickedEvent(
        String shortCode,
        String ipAddress,
        String userAgent,
        String referer) {
}