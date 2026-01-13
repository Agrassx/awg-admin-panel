#!/bin/bash

# Frontend development server with hot reload
# Proxies API requests to backend on port 8080

cd "$(dirname "$0")/.."

echo "Starting frontend dev server on http://localhost:3000"
echo "API requests will be proxied to http://localhost:8080"
echo ""
echo "Make sure backend is running: ./scripts/run-backend-dev.sh"
echo ""

./gradlew :frontend:jsBrowserDevelopmentRun --continuous
