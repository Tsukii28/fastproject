package com.example.fastproject.common.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthController {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        // 1. Kiểm tra kết nối Redis: Ghi thử một key kèm thời gian hết hạn 10 giây rồi
        // đọc lại
        String testKey = "health:ping";
        String expectedValue = "pong-" + LocalDateTime.now();
        redisTemplate.opsForValue().set(testKey, expectedValue, Duration.ofSeconds(10));
        String actualRedisValue = redisTemplate.opsForValue().get(testKey);

        // 2. Kiểm tra kết nối PostgreSQL: Chạy câu truy vấn nhẹ nhất
        Integer dbResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "redisStatus", expectedValue.equals(actualRedisValue) ? "CONNECTED" : "ERROR",
                "redisTestValue", String.valueOf(actualRedisValue),
                "postgresStatus", (dbResult != null && dbResult == 1) ? "CONNECTED" : "ERROR",
                "timestamp", LocalDateTime.now().toString()));
    }
}