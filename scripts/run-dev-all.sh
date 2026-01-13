#!/bin/bash

# Start both backend and frontend in development mode
# Uses tmux or runs sequentially

cd "$(dirname "$0")/.."

if command -v tmux &> /dev/null; then
    echo "Starting with tmux..."
    
    # Kill existing session if any
    tmux kill-session -t awg-admin 2>/dev/null
    
    # Create new session with backend
    tmux new-session -d -s awg-admin -n backend './scripts/run-backend-dev.sh'
    
    # Wait for backend to start
    sleep 5
    
    # Add frontend window
    tmux new-window -t awg-admin -n frontend './scripts/run-frontend-dev.sh'
    
    echo ""
    echo "Started in tmux session 'awg-admin'"
    echo "  - Backend:  http://localhost:8080"
    echo "  - Frontend: http://localhost:3000"
    echo ""
    echo "Attach with: tmux attach -t awg-admin"
    echo "Switch windows: Ctrl+B then 0/1"
    echo ""
    
    tmux attach -t awg-admin
else
    echo "tmux not found. Install with: apt install tmux"
    echo ""
    echo "Running backend only. Start frontend in another terminal:"
    echo "  ./scripts/run-frontend-dev.sh"
    echo ""
    
    ./scripts/run-backend-dev.sh
fi
