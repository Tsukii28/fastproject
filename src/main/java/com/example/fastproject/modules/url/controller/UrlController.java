package com.example.fastproject.modules.url.controller;

import com.example.fastproject.modules.url.dto.ShortenRequest;
import com.example.fastproject.modules.url.dto.ShortenResponse;
import com.example.fastproject.modules.url.entity.ClickAnalytics;
import com.example.fastproject.modules.url.entity.UrlMapping;
import com.example.fastproject.modules.url.event.UrlClickedEvent;
import com.example.fastproject.modules.url.repository.ClickAnalyticsRepository;
import com.example.fastproject.modules.url.repository.UrlMappingRepository;
import com.example.fastproject.modules.url.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
public class UrlController {

    private final UrlShortenerService urlShortenerService;
    private final ApplicationEventPublisher eventPublisher;
    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticsRepository analyticsRepository;

    public UrlController(UrlShortenerService urlShortenerService,
            ApplicationEventPublisher eventPublisher,
            UrlMappingRepository urlMappingRepository,
            ClickAnalyticsRepository analyticsRepository) {
        this.urlShortenerService = urlShortenerService;
        this.eventPublisher = eventPublisher;
        this.urlMappingRepository = urlMappingRepository;
        this.analyticsRepository = analyticsRepository;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request,
            HttpServletRequest httpRequest) {
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                + (httpRequest.getServerPort() == 80 || httpRequest.getServerPort() == 443
                        ? ""
                        : ":" + httpRequest.getServerPort());

        ShortenResponse response = urlShortenerService.createShortUrl(request, baseUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String originalUrl = urlShortenerService.resolveOriginalUrl(shortCode);

        // Bắn sự kiện xử lý ngầm (@Async), không làm chậm phản hồi của người dùng
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) {
            ipAddress = request.getRemoteAddr();
        }
        eventPublisher.publishEvent(new UrlClickedEvent(
                shortCode,
                ipAddress,
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.REFERER)));

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(originalUrl).toString())
                .build();
    }

    @GetMapping("/api/v1/urls/{shortCode}/stats")
    public ResponseEntity<?> getStats(@PathVariable String shortCode) {
        UrlMapping mapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy link"));

        List<ClickAnalytics> recentClicks = analyticsRepository.findTop20ByUrlIdOrderByClickedAtDesc(mapping.getId());

        return ResponseEntity.ok(Map.of(
                "shortCode", mapping.getShortCode(),
                "originalUrl", mapping.getOriginalUrl(),
                "totalClicks", mapping.getClickCount(),
                "createdAt", mapping.getCreatedAt(),
                "recentClicks", recentClicks));
    }
}