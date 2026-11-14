package com.example.fastproject.modules.url.event;

import com.example.fastproject.modules.url.entity.ClickAnalytics;
import com.example.fastproject.modules.url.repository.ClickAnalyticsRepository;
import com.example.fastproject.modules.url.repository.UrlMappingRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UrlAnalyticsListener {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticsRepository analyticsRepository;

    public UrlAnalyticsListener(UrlMappingRepository urlMappingRepository,
            ClickAnalyticsRepository analyticsRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.analyticsRepository = analyticsRepository;
    }

    @Async
    @EventListener
    @Transactional
    public void handleUrlClicked(UrlClickedEvent event) {
        urlMappingRepository.findByShortCode(event.shortCode()).ifPresent(url -> {
            // 1. Tăng tổng lượt click nguyên tử (Atomic update)
            urlMappingRepository.incrementClickCount(event.shortCode());

            // 2. Lưu log chi tiết người truy cập
            ClickAnalytics log = ClickAnalytics.builder()
                    .urlId(url.getId())
                    .ipAddress(event.ipAddress())
                    .userAgent(event.userAgent())
                    .referer(event.referer())
                    .build();
            analyticsRepository.save(log);
        });
    }
}