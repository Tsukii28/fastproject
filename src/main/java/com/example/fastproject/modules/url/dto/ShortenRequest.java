package com.example.fastproject.modules.url.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ShortenRequest(
        @NotBlank(message = "URL gốc không được để trống") @Pattern(regexp = "^(http|https)://.*$", message = "URL phải bắt đầu bằng http:// hoặc https://") String originalUrl,

        @Size(min = 4, max = 30, message = "Mã tùy chỉnh phải từ 4 đến 30 ký tự") @Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "Mã tùy chỉnh chỉ chứa chữ, số, gạch ngang và gạch dưới") String customAlias,

        LocalDateTime expiresAt) {
}