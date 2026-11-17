# ------------------------------------------------------------
# Stage 1: Build application using Maven & Java 21
# ------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven files
COPY backend/pom.xml .
COPY backend/src ./src

# Build JAR package (skip tests during container packaging)
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# ------------------------------------------------------------
# Stage 2: Minimal Java 21 Runtime
# ------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled executable JAR from builder
COPY --from=builder /app/target/realtime-chat-backend-1.0.0.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
