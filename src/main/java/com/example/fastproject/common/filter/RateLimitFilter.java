package com.example.fastproject.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:create_url:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // Chỉ áp dụng giới hạn cho API tạo link ngắn (POST /api/v1/urls)
        if (HttpMethod.POST.matches(request.getMethod()) && "/api/v1/urls".equals(request.getRequestURI())) {
            String clientIp = extractClientIp(request);
            String key = RATE_LIMIT_PREFIX + clientIp;

            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }

            if (currentCount != null && currentCount > MAX_REQUESTS_PER_MINUTE) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                String jsonError = """
                        {
                          "timestamp": "%s",
                          "status": 429,
                          "error": "Too Many Requests",
                          "message": "Bạn thao tác quá nhanh. Vui lòng thử lại sau 1 phút.",
                          "path": "/api/v1/urls"
                        }
                        """.formatted(LocalDateTime.now());
                response.getWriter().write(jsonError);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}