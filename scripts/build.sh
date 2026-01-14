#!/bin/bash

# Build script for production

set -e

cd "$(dirname "$0")/.."

echo "Building application (frontend + backend)..."
./gradlew :backend:jar

echo ""
echo "Build complete!"
echo "JAR location: backend/build/libs/backend.jar"
echo ""
echo "Run with: java -jar backend/build/libs/backend.jar"
