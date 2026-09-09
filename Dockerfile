# ==============================================================================
# SUCHARU PRO — PRODUCTION BACKEND DOCKERFILE
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="Sucharu Pro Engineering <engineering@sucharu.pro>"
LABEL description="Sucharu Pro Commercial Printing ERP Backend Runtime"

# Install curl for healthcheck
RUN apk update && apk add --no-cache curl ca-certificates && rm -rf /var/cache/apk/*

# Security: Create non-root system user (UID: 10001)
RUN addgroup -g 10001 -S sucharu && adduser -u 10001 -S sucharu -G sucharu -s /sbin/nologin

WORKDIR /app

# Copy compiled JAR (built by Gradle: `./gradlew :backend:jar`)
COPY --chown=sucharu:sucharu backend/build/libs/sucharu-server.jar /app/sucharu-server.jar

# Enforce non-root execution
USER sucharu:sucharu

ENV PORT=8080
ENV ENVIRONMENT=production
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -f http://localhost:8080/ready || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/sucharu-server.jar"]
