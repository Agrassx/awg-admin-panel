#!/bin/bash

# Backend with MOCK WireGuard interface
# Use this for local development without actual VPN

export SERVER_PORT=8080
export WG_INTERFACE=awg0-mock
export WG_ENDPOINT=demo.example.com
export DATABASE_PATH=./data/awg-admin-dev.db
export USE_MOCK_WG=true
export DNS_SERVERS=1.1.1.1,8.8.8.8

cd "$(dirname "$0")/.."

mkdir -p data

echo "╔════════════════════════════════════════════════════════╗"
echo "║  AWG Admin - Development Mode (Mock WireGuard)         ║"
echo "╠════════════════════════════════════════════════════════╣"
echo "║  Backend:  http://localhost:$SERVER_PORT                       ║"
echo "║  Frontend: http://localhost:3000 (run separately)      ║"
echo "║                                                        ║"
echo "║  WireGuard operations are SIMULATED                    ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

./gradlew :backend:run
