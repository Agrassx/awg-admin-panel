package org.awgadmin.network

import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.delay
import org.awgadmin.config.ObfuscationConfig
import org.awgadmin.domain.ObfuscationParams
import org.awgadmin.domain.ServerConfig

/**
 * Mock implementation of WgInterface for local development.
 * Simulates WireGuard operations without actual interface.
 */
class MockWgInterface(
    private val serverEndpoint: String = "localhost",
    private val serverPort: Int = 51820,
    private val serverAddress: String = "10.0.0.1/24",
    private val dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
    private val obfuscation: ObfuscationConfig = ObfuscationConfig.fromEnv(),
) : WgInterface {

    private val peers = mutableMapOf<String, MockPeer>()
    private val random = SecureRandom()
    private val mockServerPublicKey = generateRandomKey()

    override suspend fun getServerConfig(): ServerConfig {
        delay(50) // Simulate network delay
        return ServerConfig(
            interfaceName = "awg0-mock",
            publicKey = mockServerPublicKey,
            listenPort = serverPort,
            endpoint = serverEndpoint,
            subnet = serverAddress,
            dns = dns,
        )
    }

    override suspend fun getObfuscationParams(): ObfuscationParams {
        delay(50)
        return ObfuscationParams(
            jc = obfuscation.jc,
            jmin = obfuscation.jmin,
            jmax = obfuscation.jmax,
            s1 = obfuscation.s1,
            s2 = obfuscation.s2,
            h1 = obfuscation.h1,
            h2 = obfuscation.h2,
            h3 = obfuscation.h3,
            h4 = obfuscation.h4,
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
