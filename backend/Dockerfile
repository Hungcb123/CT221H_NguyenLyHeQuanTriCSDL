# =========================================================================
# Multi-stage Dockerfile cho Spring Boot 3.3 Backend (Java 21)
# =========================================================================

# Stage 1: Build source code bằng Maven
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
# Tải dependencies trước để cache layer
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime image gọn nhẹ
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Tạo non-root user đảm bảo bảo mật
RUN groupadd -r spring && useradd -r -g spring spring \
    && mkdir -p /app/.local && chown spring:spring /app/.local
USER spring:spring

COPY --from=build /app/target/fresh-grocery-1.0.0.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
