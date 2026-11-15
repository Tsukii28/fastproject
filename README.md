# SnapLink — Hệ thống Rút gọn URL Hiệu năng cao & Phân tích Thời gian thực

Dịch vụ rút gọn liên kết chuẩn Production được xây dựng trên nền tảng **Spring Boot**, **PostgreSQL** và **Redis**. Hệ thống được thiết kế để chuyển hướng với độ trễ cực thấp thông qua mô hình Cache-Aside, thống kê lượt truy cập bất đồng bộ theo hướng sự kiện (Event-driven) và giới hạn tần suất truy cập (Rate Limiting) theo địa chỉ IP bằng Redis.

---

## Kiến trúc Hệ thống

```text
                         +-----------------------+
                         |   Giao diện / Client  |
                         +-----------+-----------+
                                     |
                                     v
                         +-----------------------+
                         |   RateLimitFilter     |  (Bộ đếm Fixed-Window trên Redis)
                         +-----------+-----------+
                                     |
                                     v
+-------------------------------------------------------------------------+
|                        Ứng dụng Spring Boot                             |
|                                                                         |
|  [POST /api/v1/urls] ----> Bộ mã hóa Base62 ----> PostgreSQL (Flyway)   |
|                                  |                                      |
|                                  +------------> Redis Cache (TTL 24h)   |
|                                                                         |
|  [GET /{shortCode}]  ----> Truy vấn Cache-Aside                         |
|                              |-- Hit  --> Redis (Phản hồi dưới 1ms)     |
|                              |-- Miss --> PostgreSQL -> Lưu vào Cache   |
|                              |                                          |
|                              +--> Phát sự kiện UrlClickedEvent          |
|                                         |                               |
|                                         v (@Async Luồng chạy ngầm)      |
|                                   UrlAnalyticsListener                  |
|                                    |-- Cập nhật nguyên tử số lượt click |
|                                    +-- Lưu log Thiết bị / IP / Referer  |
+-------------------------------------------------------------------------+



**Điểm nhấn Kỹ thuật Nổi bật**

    - Mã hóa Base62 không trùng lặp: Chuyển đổi ID tự tăng từ cơ sở dữ liệu kết hợp giá trị bù (offset) thành chuỗi mã rút gọn an toàn trên URL (0-9, a-z, A-Z), đồng thời hỗ trợ đặt mã tùy chỉnh (customAlias) và thời gian hết hạn (expiresAt).

    - Cơ chế chuyển hướng Cache-Aside: Phục vụ trực tiếp các liên kết truy cập nhiều từ bộ nhớ đệm Redis (GET /{shortCode}) với thời gian sống (TTL) đồng bộ linh hoạt theo hạn sử dụng của link, loại bỏ tải đọc đĩa từ database khi lưu lượng tăng đột biến.

    - Phân tích thống kê bất đồng bộ (Non-blocking): Tách biệt hoàn toàn luồng lưu trữ dữ liệu thống kê khỏi luồng chuyển hướng chính bằng Spring Application Events (@EventListener) chạy trên luồng @Async, kết hợp cập nhật nguyên tử dưới SQL (click_count = click_count + 1) để tránh xung đột dữ liệu (race condition).

    - Chống spam phân tán (Rate Limiting): Bảo vệ API tạo liên kết khỏi hành vi gửi yêu cầu hàng loạt thông qua bộ lọc OncePerRequestFilter sử dụng bộ đếm nguyên tử trên Redis (trả về mã 429 Too Many Requests).

    - Chuẩn hóa API & Tài liệu tương tác: Xử lý ngoại lệ tập trung (@RestControllerAdvice) trả về định dạng JSON đồng nhất, tích hợp sẵn Swagger UI và giao diện điều khiển Single-page chạy cùng domain (không phát sinh lỗi CORS).

    - Đóng gói chuẩn Production: Sử dụng Multi-stage Dockerfile chạy trên nền Alpine JRE với quyền user thường (non-root) để tối ưu dung lượng và bảo mật, điều phối trọn gói cùng PostgreSQL 16 và Redis 7 qua Docker Compose.


**Công nghệ Sử dụng**

  *  Backend: Java 21, Spring Boot, Spring Data JPA, Spring Validation

  *  Cơ sở dữ liệu & Migration: PostgreSQL 16, Flyway

  *  Bộ nhớ đệm & Rate Limiting: Redis 7 (StringRedisTemplate)

  *  Tài liệu API: Springdoc OpenAPI (Swagger UI)

  *  Giao diện (Frontend): HTML5, Tailwind CSS, Vanilla JavaScript

  *  DevOps: Docker, Multi-stage Docker Build, Docker Compose

  