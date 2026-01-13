#!/bin/bash

# Development run script
# Starts backend with hot reload

export SERVER_PORT=8080
export WG_INTERFACE=awg0
export WG_ENDPOINT=$(curl -s ifconfig.me)
export DATABASE_PATH=./data/awg-admin.db
export AWG_BINARY=awg
export WG_BINARY=wg
export DNS_SERVERS=1.1.1.1,8.8.8.8

mkdir -p data

cd "$(dirname "$0")/.."
./gradlew :backend:run
