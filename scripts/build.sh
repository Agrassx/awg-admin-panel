#!/bin/bash

# Build script for production

set -e

cd "$(dirname "$0")/.."

echo "Building frontend..."
./gradlew :frontend:jsBrowserProductionWebpack

echo "Building backend..."
./gradlew :backend:jar

echo "Copying frontend to backend resources..."
mkdir -p backend/build/resources/main/static
cp -r frontend/build/dist/js/productionExecutable/* backend/build/resources/main/static/

echo "Build complete!"
echo "Run with: java -jar backend/build/libs/backend.jar"
