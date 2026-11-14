package com.example.fastproject.modules.url.service;

import com.example.fastproject.modules.url.dto.ShortenRequest;
import com.example.fastproject.modules.url.dto.ShortenResponse;
import com.example.fastproject.modules.url.entity.UrlMapping;
import com.example.fastproject.modules.url.repository.UrlMappingRepository;
import com.example.fastproject.modules.url.util.Base62Encoder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UrlShortenerService {

    private static final String CACHE_PREFIX = "url:code:";
    private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(24);

    private final UrlMappingRepository repository;
    private final Base62Encoder base62Encoder;
    private final StringRedisTemplate redisTemplate;

    public UrlShortenerService(UrlMappingRepository repository,
            Base62Encoder base62Encoder,
            StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.base62Encoder = base62Encoder;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public ShortenResponse createShortUrl(ShortenRequest request, String baseUrl) {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(request.originalUrl());
        mapping.setExpiresAt(request.expiresAt());

        if (StringUtils.hasText(request.customAlias())) {
            if (repository.existsByShortCode(request.customAlias())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã tùy chỉnh đã tồn tại");
            }
            mapping.setShortCode(request.customAlias());
            mapping = repository.save(mapping);
        } else {
            // Lưu tạm để lấy ID tự tăng từ PostgreSQL, sau đó mã hóa Base62
            mapping.setShortCode("tmp_" + UUID.randomUUID().toString().substring(0, 8));
            mapping = repository.save(mapping);
            // Cộng offset 100000 để mã rút gọn tối thiểu 4 ký tự cho đẹp
            String generatedCode = base62Encoder.encode(mapping.getId() + 100000L);
            mapping.setShortCode(generatedCode);
            mapping = repository.save(mapping);
        }

        // Đẩy sẵn vào Redis Cache
        saveToCache(mapping.getShortCode(), mapping.getOriginalUrl(), mapping.getExpiresAt());

        return new ShortenResponse(
                mapping.getShortCode(),
                baseUrl + "/" + mapping.getShortCode(),
                mapping.getOriginalUrl(),
                mapping.getExpiresAt(),
                mapping.getCreatedAt());
    }

    public String resolveOriginalUrl(String shortCode) {
        String cacheKey = CACHE_PREFIX + shortCode;

        // 1. Tìm trong Redis Cache trước (Cache Hit)
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            return cachedUrl;
        }

        // 2. Không có trong Cache -> Truy vấn PostgreSQL (Cache Miss)
        UrlMapping mapping = repository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đường dẫn"));

        if (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Đường dẫn đã hết hạn sử dụng");
        }

        // 3. Lưu ngược lại vào Redis cho các lần truy cập sau
        saveToCache(shortCode, mapping.getOriginalUrl(), mapping.getExpiresAt());
        return mapping.getOriginalUrl();
    }

    private void saveToCache(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        Duration ttl = DEFAULT_CACHE_TTL;
        if (expiresAt != null) {
            Duration untilExpire = Duration.between(LocalDateTime.now(), expiresAt);
            if (untilExpire.isNegative() || untilExpire.isZero()) {
                return;
            }
            ttl = untilExpire.compareTo(DEFAULT_CACHE_TTL) < 0 ? untilExpire : DEFAULT_CACHE_TTL;
        }
        redisTemplate.opsForValue().set(CACHE_PREFIX + shortCode, originalUrl, ttl);
    }
}