# Build stage - Frontend
FROM gradle:8.12-jdk21 AS frontend-build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY frontend ./frontend
RUN gradle :frontend:jsBrowserProductionWebpack --no-daemon

# Build stage - Backend
FROM gradle:8.12-jdk21 AS backend-build
WORKDIR /app
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY backend ./backend
RUN gradle :backend:jar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache bash curl

WORKDIR /app

# Copy backend jar
COPY --from=backend-build /app/backend/build/libs/*.jar app.jar

# Copy frontend static files
COPY --from=frontend-build /app/frontend/build/dist/js/productionExecutable/ ./static/

# Create data directory
RUN mkdir -p /app/data

# Environment variables
ENV SERVER_PORT=8080
ENV WG_INTERFACE=awg0
ENV WG_ENDPOINT=localhost
ENV DATABASE_PATH=/app/data/awg-admin.db
ENV AWG_BINARY=/usr/bin/awg
ENV WG_BINARY=/usr/bin/wg
ENV DNS_SERVERS=1.1.1.1,8.8.8.8

EXPOSE 8080

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
