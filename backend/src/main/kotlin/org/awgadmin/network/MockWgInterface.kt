package org.awgadmin.network

import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.delay
import org.awgadmin.domain.ObfuscationParams
import org.awgadmin.domain.ServerConfig

/**
 * Mock implementation of WgInterface for local development.
 * Simulates WireGuard operations without actual interface.
 */
class MockWgInterface(
    private val serverEndpoint: String = "localhost",
    private val dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
) : WgInterface {

    private val peers = mutableMapOf<String, MockPeer>()
    private val random = SecureRandom()

    override suspend fun getServerConfig(): ServerConfig {
        delay(50) // Simulate network delay
        return ServerConfig(
            interfaceName = "awg0-mock",
            publicKey = "MockServerPublicKey123456789012345678901234=",
            listenPort = 443,
            endpoint = serverEndpoint,
            subnet = "10.66.66.0/24",
            dns = dns,
        )
    }

    override suspend fun getObfuscationParams(): ObfuscationParams {
        delay(50)
        return ObfuscationParams(
            jc = 8,
            jmin = 40,
            jmax = 1000,
            s1 = 89,
            s2 = 117,
            h1 = 1735261843L,
            h2 = 983742156L,
            h3 = 572849301L,
            h4 = 294817365L,
        )
    }

    override suspend fun addPeer(
        publicKey: String,
        presharedKey: String,
        allowedIps: String,
    ) {
        delay(100)
        peers[publicKey] = MockPeer(
            publicKey = publicKey,
            presharedKey = presharedKey,
            allowedIps = allowedIps,
            addedAt = System.currentTimeMillis() / 1000,
        )
    }

    override suspend fun removePeer(publicKey: String) {
        delay(100)
        peers.remove(publicKey)
    }

    override suspend fun getPeersStatus(): List<PeerInfo> {
        delay(50)
        val now = System.currentTimeMillis() / 1000

        return peers.values.map { peer ->
            // Simulate random online/offline status
            val isOnline = random.nextBoolean()
            val lastHandshake = if (isOnline) now - random.nextInt(60) else null

            PeerInfo(
                publicKey = peer.publicKey,
                endpoint = if (isOnline) "192.168.1.${random.nextInt(254) + 1}:${random.nextInt(60000) + 1024}" else null,
                allowedIps = peer.allowedIps,
                latestHandshake = lastHandshake,
                transferRx = if (isOnline) random.nextLong(1024 * 1024 * 100) else 0L,
                transferTx = if (isOnline) random.nextLong(1024 * 1024 * 50) else 0L,
            )
        }
    }

    override suspend fun generateKeyPair(): KeyPair {
        delay(50)
        return KeyPair(
            privateKey = generateRandomKey(),
            publicKey = generateRandomKey(),
        )
    }

    override suspend fun generatePresharedKey(): String {
        delay(50)
        return generateRandomKey()
    }

    override suspend fun reload() {
        delay(100)
    }

    private fun generateRandomKey(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    private data class MockPeer(
        val publicKey: String,
        val presharedKey: String,
        val allowedIps: String,
        val addedAt: Long,
    )
}
