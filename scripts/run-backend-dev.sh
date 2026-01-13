#!/bin/bash

# Backend development server
# Runs on port 8080 with auto-reload on code changes

export SERVER_PORT=8080
export WG_INTERFACE=${WG_INTERFACE:-awg0}
export WG_ENDPOINT=${WG_ENDPOINT:-$(curl -s ifconfig.me 2>/dev/null || echo "localhost")}
export DATABASE_PATH=./data/awg-admin.db
export AWG_BINARY=${AWG_BINARY:-awg}
export WG_BINARY=${WG_BINARY:-wg}
export DNS_SERVERS=1.1.1.1,8.8.8.8

cd "$(dirname "$0")/.."

mkdir -p data

echo "Starting backend on http://localhost:$SERVER_PORT"
echo "WG Interface: $WG_INTERFACE"
echo "WG Endpoint: $WG_ENDPOINT"
echo ""

./gradlew :backend:run --continuous
