# =============================================================================
# Stage 1: Build AmneziaWG from Go sources
# =============================================================================
FROM golang:1.24-bookworm AS awg-build

WORKDIR /build

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    make \
    && rm -rf /var/lib/apt/lists/*

# Clone and build amneziawg-go (userspace implementation)
RUN git clone --depth 1 https://github.com/amnezia-vpn/amneziawg-go.git \
    && cd amneziawg-go \
    && make

# Clone and build amneziawg-tools (awg, awg-quick utilities)
RUN git clone --depth 1 https://github.com/amnezia-vpn/amneziawg-tools.git \
    && cd amneziawg-tools/src \
    && make

# =============================================================================
# Stage 2: Build Java application (frontend + backend)
# =============================================================================
FROM gradle:8.12-jdk21 AS app-build

WORKDIR /app

# Copy project files
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY frontend ./frontend
COPY backend ./backend

# Build frontend and backend JAR
RUN gradle :frontend:jsBrowserProductionWebpack :backend:jar --no-daemon

# =============================================================================
# Stage 3: Runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-jammy

# Install runtime dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    curl \
    iproute2 \
    iptables \
    procps \
    openresolv \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /dev/net \
    && mknod /dev/net/tun c 10 200 || true

WORKDIR /app

# Copy AmneziaWG binaries
COPY --from=awg-build /build/amneziawg-go/amneziawg-go /usr/bin/amneziawg-go
COPY --from=awg-build /build/amneziawg-tools/src/wg /usr/bin/awg
COPY --from=awg-build /build/amneziawg-tools/src/wg-quick/linux.bash /usr/bin/awg-quick

# Make binaries executable
RUN chmod +x /usr/bin/amneziawg-go /usr/bin/awg /usr/bin/awg-quick

# Copy backend JAR (contains frontend static files)
COPY --from=app-build /app/backend/build/libs/awg-admin.jar app.jar

# Copy entrypoint script
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Create directories
RUN mkdir -p /app/data /etc/amnezia/amneziawg

# Environment variables
ENV SERVER_PORT=8080
ENV WG_INTERFACE=awg0
ENV WG_ENDPOINT=""
ENV WG_PORT=51820
ENV WG_ADDRESS=10.0.0.1/24
ENV WG_ALLOWED_IPS=10.0.0.0/24
ENV DATABASE_PATH=/app/data/awg-admin.db
ENV AWG_BINARY=/usr/bin/awg
ENV WG_BINARY=/usr/bin/awg
ENV DNS_SERVERS=1.1.1.1,8.8.8.8
ENV SESSION_SECRET=""
ENV USE_MOCK_WG=false

# AmneziaWG obfuscation settings (default values for high obfuscation)
ENV AWG_JC=4
ENV AWG_JMIN=40
ENV AWG_JMAX=70
ENV AWG_S1=55
ENV AWG_S2=55
ENV AWG_H1=1234567891
ENV AWG_H2=1234567892
ENV AWG_H3=1234567893
ENV AWG_H4=1234567894

# JVM options
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
EXPOSE 51820/udp

VOLUME ["/app/data", "/etc/amnezia/amneziawg"]

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/auth/me || exit 1

ENTRYPOINT ["/entrypoint.sh"]
