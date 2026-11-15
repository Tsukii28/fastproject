# Giai đoạn 1: Build file JAR bằng JDK 25
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
# Tải sẵn thư viện để tận dụng Docker Layer Cache
RUN chmod +x mvnw && ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng bằng JRE 25 siêu nhẹ (~180MB)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Tạo user thường (non-root) để tăng cường bảo mật container
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]