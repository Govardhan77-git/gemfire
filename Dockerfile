# ============================================================================
# Multi-stage Dockerfile for GemFire Spring Boot Demo
# ============================================================================

# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom first for layer caching — dependencies only re-download if pom changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="GemFire Demo Team"
LABEL description="Spring Boot + Broadcom GemFire In-Memory Demo"
LABEL version="1.0.0"

# Create non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy built artifact from builder stage
COPY --from=builder /build/target/gemfire-springboot-demo-*.jar app.jar

# GemFire needs a writable directory for its disk store and logs
RUN mkdir -p /app/gemfire-data && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

# JVM tuning for containers: respect container memory limits
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
