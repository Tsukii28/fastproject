CREATE TABLE url_mapping (
    id BIGSERIAL PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(30) UNIQUE NOT NULL,
    click_count BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Đánh index trên short_code để tăng tốc độ tìm kiếm khi redirect
CREATE INDEX idx_url_mapping_short_code ON url_mapping(short_code);

CREATE TABLE click_analytics (
    id BIGSERIAL PRIMARY KEY,
    url_id BIGINT NOT NULL REFERENCES url_mapping(id) ON DELETE CASCADE,
    ip_address VARCHAR(45),
    user_agent TEXT,
    referer TEXT,
    clicked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index phục vụ truy vấn thống kê lượt click theo từng link và khoảng thời gian
CREATE INDEX idx_click_analytics_url_id_time ON click_analytics(url_id, clicked_at DESC);