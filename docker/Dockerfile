# Build stage - single stage with both frontend and backend
FROM gradle:8.12-jdk21 AS build
WORKDIR /app

# Copy all project files
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY frontend ./frontend
COPY backend ./backend

# Build backend JAR (includes frontend via processResources)
RUN gradle :backend:jar --no-daemon

# Runtime stage - using Debian-based image for glibc compatibility with awg/wg binaries
FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy backend jar (contains frontend static files)
COPY --from=build /app/backend/build/libs/*.jar app.jar

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
ENV SESSION_SECRET=""

EXPOSE 8080

VOLUME ["/app/data"]

ENTRYPOINT ["java", "-jar", "app.jar"]
