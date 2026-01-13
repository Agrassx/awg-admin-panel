package org.awgadmin.network

import org.awgadmin.domain.ObfuscationParams
import org.awgadmin.domain.ServerConfig

/**
 * Abstraction for WireGuard/AmneziaWG interface operations.
 * Implementations can use shell commands, native libraries, or mock for testing.
 */
interface WgInterface {

    /**
     * Get server configuration from the interface.
     */
    suspend fun getServerConfig(): ServerConfig

    /**
     * Get obfuscation parameters from the interface.
     */
    suspend fun getObfuscationParams(): ObfuscationParams

    /**
     * Add a peer to the interface.
     */
    suspend fun addPeer(
        publicKey: String,
        presharedKey: String,
        allowedIps: String,
    )

    /**
     * Remove a peer from the interface.
     */
    suspend fun removePeer(publicKey: String)

    /**
     * Get status of all connected peers.
     */
    suspend fun getPeersStatus(): List<PeerInfo>

    /**
     * Generate a new key pair.
     */
    suspend fun generateKeyPair(): KeyPair

    /**
     * Generate a preshared key.
     */
    suspend fun generatePresharedKey(): String

    /**
     * Reload the interface configuration.
     */
    suspend fun reload()
}

/**
 * Key pair for WireGuard.
 */
data class KeyPair(
    val privateKey: String,
    val publicKey: String,
)

/**
 * Raw peer info from the interface.
 */
data class PeerInfo(
    val publicKey: String,
    val endpoint: String?,
    val allowedIps: String,
    val latestHandshake: Long?,
    val transferRx: Long,
    val transferTx: Long,
)
