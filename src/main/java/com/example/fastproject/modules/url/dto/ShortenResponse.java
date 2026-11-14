package com.example.fastproject.modules.url.dto;

import java.time.LocalDateTime;

public record ShortenResponse(
        String shortCode,
        String shortUrl,
        String originalUrl,
        LocalDateTime expiresAt,
        LocalDateTime createdAt) {
}