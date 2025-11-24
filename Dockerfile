# ══════════════════════════════════════════════════════════════
# NexusAI - Production Dockerfile
# Multi-stage build for optimized container image
# ══════════════════════════════════════════════════════════════

# ────────────────────────────────────────────────────────────────
# Stage 1: Build
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY nexus-commons/pom.xml nexus-commons/
COPY nexus-core/pom.xml nexus-core/
COPY nexus-auth/pom.xml nexus-auth/
COPY nexus-companion/pom.xml nexus-companion/
COPY nexus-conversation/pom.xml nexus-conversation/
COPY nexus-ai-engine/pom.xml nexus-ai-engine/
COPY nexus-media/pom.xml nexus-media/
COPY nexus-moderation/pom.xml nexus-moderation/
COPY nexus-analytics/pom.xml nexus-analytics/
COPY nexus-payment/pom.xml nexus-payment/
COPY nexus-api/pom.xml nexus-api/

# Download dependencies (cached layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code
COPY nexus-commons/src nexus-commons/src
COPY nexus-core/src nexus-core/src
COPY nexus-auth/src nexus-auth/src
COPY nexus-companion/src nexus-companion/src
COPY nexus-conversation/src nexus-conversation/src
COPY nexus-ai-engine/src nexus-ai-engine/src
COPY nexus-media/src nexus-media/src
COPY nexus-moderation/src nexus-moderation/src
COPY nexus-analytics/src nexus-analytics/src
COPY nexus-payment/src nexus-payment/src
COPY nexus-api/src nexus-api/src

# Build the application
RUN ./mvnw clean package -DskipTests -B

# Extract layers for optimized startup
RUN java -Djarmode=layertools -jar nexus-api/target/*.jar extract --destination extracted

# ────────────────────────────────────────────────────────────────
# Stage 2: Runtime
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: Create non-root user
RUN addgroup -g 1001 -S nexusai && \
    adduser -u 1001 -S nexusai -G nexusai

WORKDIR /app

# Copy extracted layers
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Set ownership
RUN chown -R nexusai:nexusai /app

# Switch to non-root user
USER nexusai

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# JVM Options for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=prod"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
