#!/bin/bash

# Full development environment with Mock WireGuard
# Backend + Frontend with hot reload

cd "$(dirname "$0")/.."

if ! command -v tmux &> /dev/null; then
    echo "Installing tmux..."
    apt install -y tmux 2>/dev/null || brew install tmux 2>/dev/null || {
        echo "Please install tmux manually"
        exit 1
    }
fi

# Kill existing session
tmux kill-session -t awg-dev 2>/dev/null

echo "╔════════════════════════════════════════════════════════╗"
echo "║  AWG Admin - Full Development Environment              ║"
echo "╠════════════════════════════════════════════════════════╣"
echo "║  Backend:  http://localhost:8080 (Mock WG)             ║"
echo "║  Frontend: http://localhost:3000 (Hot Reload)          ║"
echo "╚════════════════════════════════════════════════════════╝"
echo ""

# Create session with backend
tmux new-session -d -s awg-dev -n backend './scripts/run-backend-mock.sh; read'

# Wait for backend to initialize
sleep 3

# Add frontend window
tmux new-window -t awg-dev -n frontend './scripts/run-frontend-dev.sh; read'

echo "Started in tmux session 'awg-dev'"
echo ""
echo "Commands:"
echo "  Attach:          tmux attach -t awg-dev"
echo "  Switch windows:  Ctrl+B then 0 (backend) or 1 (frontend)"
echo "  Detach:          Ctrl+B then D"
echo "  Kill:            tmux kill-session -t awg-dev"
echo ""

tmux attach -t awg-dev
