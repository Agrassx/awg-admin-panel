# AWG Admin

> Web admin panel for managing AmneziaWG VPN clients

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Ktor-3.0-087CFA?logo=ktor" alt="Ktor">
  <img src="https://img.shields.io/badge/React-18-61DAFB?logo=react" alt="React">
  <img src="https://img.shields.io/badge/MUI-6-007FFF?logo=mui" alt="MUI">
</p>

## 📋 What is it?

**AWG Admin** is a web panel for managing VPN clients on a server with [AmneziaWG](https://amnezia.org/) installed. It allows you to create, delete, and manage clients through a convenient interface without needing console access.

## 🎯 Use Cases

- Manage VPN clients without SSH access to the server
- Monitor online/offline status of clients
- Quickly create configs with QR codes for AmneziaVPN
- Control certificate expiration dates
- View traffic statistics

## ✨ Features

| Feature                  | Description                              |
|--------------------------|------------------------------------------|
| 👥 **Client Management** | Create, delete, enable/disable clients   |
| 📊 **Monitoring**        | Online/offline status, last handshake    |
| 📈 **Statistics**        | Incoming/outgoing traffic per client     |
| ⏰ **Expiration**        | Set certificate expiration date          |
| 📱 **QR Codes**          | Generate configs for AmneziaVPN app      |
| 📋 **Export**            | Download .conf files                     |
| 🌙 **Dark UI**           | Modern dark Material Design 3 interface  |

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│                 │     │                 │     │                 │
│  React + MUI    │────▶│  Ktor Backend   │────▶│  AmneziaWG      │
│  (Frontend)     │     │  (REST API)     │     │  (awg/wg)       │
│                 │     │                 │     │                 │
└─────────────────┘     └────────┬────────┘     └─────────────────┘
                                │
                       ┌────────▼────────┐
                       │                 │
                       │  SQLite + ORM   │
                       │  (Exposed)      │
                       │                 │
                       └─────────────────┘
```

### Tech Stack

- **Backend:** Kotlin + Ktor (lightweight framework)
- **Frontend:** Kotlin/JS + React + MUI (Material Design 3)
- **Database:** SQLite + Exposed ORM
- **WireGuard:** Abstraction via interface (Shell/Mock)

All dependency versions are centralized in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## 🚀 Getting Started

### Requirements

- Java 21+
- AmneziaWG installed and configured (`awg`, `wg` in PATH)
- Running interface (e.g., `awg0`)

### Production (Docker)

```bash
cd docker

# Configure environment
export WG_INTERFACE=awg0
export WG_ENDPOINT=your-server-ip

# Run
docker-compose up -d
```

Panel will be available at `http://localhost:8080`

### Production (without Docker)

```bash
# Build
./scripts/build.sh

# Run
export WG_INTERFACE=awg0
export WG_ENDPOINT=$(curl -s ifconfig.me)
java -jar backend/build/libs/backend.jar
```

### Development

```bash
# Full dev environment with Mock WireGuard
./scripts/run-dev-mock.sh

# Or separately:
# Terminal 1 - Backend
./scripts/run-backend-mock.sh

# Terminal 2 - Frontend (hot reload)
./scripts/run-frontend-dev.sh
```

## ⚙️ Configuration

All settings via environment variables:

| Variable        | Description                    | Default               |
|-----------------|--------------------------------|-----------------------|
| `SERVER_PORT`   | Web server port                | `8080`                |
| `WG_INTERFACE`  | WireGuard interface name       | `awg0`                |
| `WG_ENDPOINT`   | Server public IP/domain        | `localhost`           |
| `DATABASE_PATH` | Path to SQLite database        | `./data/awg-admin.db` |
| `AWG_BINARY`    | Path to awg binary             | `awg`                 |
| `WG_BINARY`     | Path to wg binary              | `wg`                  |
| `DNS_SERVERS`   | DNS servers for clients        | `1.1.1.1,8.8.8.8`     |
| `USE_MOCK_WG`   | Use mock mode (for dev)        | `false`               |

## 🔌 Integration

### 1. Connecting to Existing AmneziaWG

Make sure AmneziaWG is running:

```bash
awg show
```

Run the panel with correct parameters:

```bash
export WG_INTERFACE=awg0  # your interface name
export WG_ENDPOINT=127.0.0.1  # your server IP
./scripts/run-backend-dev.sh
```

### 2. API Endpoints

| Method   | Endpoint                   | Description              |
|----------|----------------------------|--------------------------|
| `GET`    | `/api/clients`             | List clients with status |
| `POST`   | `/api/clients`             | Create client            |
| `PATCH`  | `/api/clients/{id}`        | Update client            |
| `DELETE` | `/api/clients/{id}`        | Delete client            |
| `POST`   | `/api/clients/{id}/toggle` | Enable/disable client    |
| `GET`    | `/api/clients/{id}/config` | Get client config        |
| `GET`    | `/api/server/config`       | Server config            |
| `GET`    | `/api/server/obfuscation`  | Obfuscation parameters   |

### 3. API Example

```bash
# Create client
curl -X POST http://localhost:8080/api/clients \
  -H "Content-Type: application/json" \
  -d '{"name": "Client", "expiresAt": "2025-12-31T00:00:00Z"}'

# Get config
curl http://localhost:8080/api/clients/{id}/config
```

## 🔧 Debugging

### Frontend (React + MUI)

```bash
# Run with hot reload
./scripts/run-frontend-dev.sh

# Open http://localhost:3000
# Changes apply instantly
```

**DevTools:**
- React Developer Tools (Chrome/Firefox extension)
- Network tab for API debugging
- Console for logs

### Backend (Ktor)

```bash
# Run with mock WireGuard (no real VPN)
./scripts/run-backend-mock.sh

# Or with real WG
./scripts/run-backend-dev.sh
```

**Logging:**
- Logs output to console
- Log level configurable in `logback.xml`
- WireGuard errors output to stderr

### Full Debug Setup

```bash
# Run everything in tmux
./scripts/run-dev-mock.sh

# Switch windows: Ctrl+B, then 0 or 1
# Detach: Ctrl+B, then D
# Attach: tmux attach -t awg-dev
```

### Mock Mode

When `USE_MOCK_WG=true`:
- Clients are created in database but not added to WireGuard
- Online/offline status is randomly generated
- Traffic is simulated
- Keys are generated as fake

This allows debugging UI without a configured VPN.

## 📄 License

MIT
