#!/bin/bash
set -e

echo "========================================"
echo "  AWG Admin Panel - Starting..."
echo "========================================"

# Function to generate server keys if not exist
generate_server_keys() {
    local config_dir="/etc/amnezia/amneziawg"
    local private_key_file="$config_dir/server_private.key"
    local public_key_file="$config_dir/server_public.key"
    
    if [ ! -f "$private_key_file" ]; then
        echo "[INFO] Generating server keys..."
        mkdir -p "$config_dir"
        awg genkey | tee "$private_key_file" | awg pubkey > "$public_key_file"
        chmod 600 "$private_key_file"
        echo "[INFO] Server keys generated"
    fi
    
    export SERVER_PRIVATE_KEY=$(cat "$private_key_file")
    export SERVER_PUBLIC_KEY=$(cat "$public_key_file")
}

# Function to create WireGuard config
create_wg_config() {
    local config_file="/etc/amnezia/amneziawg/${WG_INTERFACE}.conf"
    
    if [ ! -f "$config_file" ]; then
        echo "[INFO] Creating WireGuard config: $config_file"
        
        cat > "$config_file" << EOF
[Interface]
PrivateKey = ${SERVER_PRIVATE_KEY}
Address = ${WG_ADDRESS}
ListenPort = ${WG_PORT}
SaveConfig = false

# AmneziaWG obfuscation parameters
Jc = ${AWG_JC}
Jmin = ${AWG_JMIN}
Jmax = ${AWG_JMAX}
S1 = ${AWG_S1}
S2 = ${AWG_S2}
H1 = ${AWG_H1}
H2 = ${AWG_H2}
H3 = ${AWG_H3}
H4 = ${AWG_H4}

# NAT and routing (will be set up by PostUp/PostDown if needed)
EOF
        
        chmod 600 "$config_file"
        echo "[INFO] WireGuard config created"
    fi
}

# Function to start WireGuard interface
start_wireguard() {
    local config_file="/etc/amnezia/amneziawg/${WG_INTERFACE}.conf"
    
    # Check if interface already exists
    if ip link show "$WG_INTERFACE" &> /dev/null; then
        echo "[INFO] WireGuard interface $WG_INTERFACE already exists"
        return 0
    fi
    
    echo "[INFO] Starting WireGuard interface: $WG_INTERFACE"
    
    # Create TUN device if not exists
    if [ ! -e /dev/net/tun ]; then
        mkdir -p /dev/net
        mknod /dev/net/tun c 10 200
        chmod 666 /dev/net/tun
    fi
    
    # Start userspace WireGuard daemon in background
    WG_QUICK_USERSPACE_IMPLEMENTATION=/usr/bin/amneziawg-go \
        awg-quick up "$config_file" || {
            echo "[WARN] awg-quick failed, trying manual setup..."
            
            # Manual setup
            ip link add dev "$WG_INTERFACE" type wireguard 2>/dev/null || \
                /usr/bin/amneziawg-go "$WG_INTERFACE" &
            
            sleep 2
            
            awg setconf "$WG_INTERFACE" <(grep -v "^Address\|^DNS\|^MTU\|^Table\|^PreUp\|^PostUp\|^PreDown\|^PostDown\|^SaveConfig" "$config_file" | grep -v "^\s*$")
            
            # Set address
            local address=$(grep "^Address" "$config_file" | cut -d= -f2 | tr -d ' ')
            ip addr add "$address" dev "$WG_INTERFACE" 2>/dev/null || true
            
            ip link set "$WG_INTERFACE" up
        }
    
    echo "[INFO] WireGuard interface $WG_INTERFACE started"
    
    # Show interface info
    awg show "$WG_INTERFACE" 2>/dev/null || echo "[WARN] Could not show interface info"
}

# Function to setup NAT/routing
setup_routing() {
    echo "[INFO] Setting up NAT/routing..."
    
    # Enable IP forwarding
    echo 1 > /proc/sys/net/ipv4/ip_forward
    
    # Get the default route interface (external interface)
    local ext_iface=$(ip route | grep default | awk '{print $5}' | head -1)
    
    if [ -n "$ext_iface" ]; then
        echo "[INFO] External interface: $ext_iface"
        
        # Setup NAT masquerading
        iptables -t nat -A POSTROUTING -s "${WG_ADDRESS%/*}/24" -o "$ext_iface" -j MASQUERADE 2>/dev/null || true
        
        # Allow forwarding
        iptables -A FORWARD -i "$WG_INTERFACE" -o "$ext_iface" -j ACCEPT 2>/dev/null || true
        iptables -A FORWARD -i "$ext_iface" -o "$WG_INTERFACE" -m state --state RELATED,ESTABLISHED -j ACCEPT 2>/dev/null || true
        
        echo "[INFO] NAT/routing configured"
    else
        echo "[WARN] Could not determine external interface, skipping NAT setup"
    fi
}

# Main entrypoint logic
main() {
    # If USE_MOCK_WG is true, skip WireGuard setup
    if [ "$USE_MOCK_WG" = "true" ]; then
        echo "[WARN] Running in MOCK mode - WireGuard disabled"
    else
        # Generate keys and create config
        generate_server_keys
        create_wg_config
        
        # Check for required capabilities
        if ! capsh --print 2>/dev/null | grep -q "cap_net_admin"; then
            echo "[WARN] Container may not have NET_ADMIN capability"
            echo "[WARN] Run with: docker run --cap-add=NET_ADMIN --cap-add=SYS_MODULE"
        fi
        
        # Start WireGuard
        start_wireguard
        
        # Setup routing if requested
        if [ "${ENABLE_NAT:-true}" = "true" ]; then
            setup_routing
        fi
        
        echo "[INFO] Server Public Key: $SERVER_PUBLIC_KEY"
    fi
    
    # Export endpoint for the application
    if [ -z "$WG_ENDPOINT" ]; then
        # Try to detect public IP
        WG_ENDPOINT=$(curl -s --max-time 5 https://api.ipify.org 2>/dev/null || echo "YOUR_SERVER_IP")
        export WG_ENDPOINT
    fi
    
    echo "[INFO] WireGuard Endpoint: ${WG_ENDPOINT}:${WG_PORT}"
    echo "========================================"
    echo "[INFO] Starting AWG Admin on port $SERVER_PORT"
    echo "========================================"
    
    # Start Java application
    exec java $JAVA_OPTS -jar app.jar
}

# Handle signals for graceful shutdown
trap 'echo "[INFO] Shutting down..."; awg-quick down /etc/amnezia/amneziawg/${WG_INTERFACE}.conf 2>/dev/null || true; exit 0' SIGTERM SIGINT

main "$@"
