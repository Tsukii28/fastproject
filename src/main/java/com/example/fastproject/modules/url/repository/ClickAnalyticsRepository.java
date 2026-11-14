package com.example.fastproject.modules.url.repository;

import com.example.fastproject.modules.url.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {
    List<ClickAnalytics> findTop20ByUrlIdOrderByClickedAtDesc(Long urlId);
}