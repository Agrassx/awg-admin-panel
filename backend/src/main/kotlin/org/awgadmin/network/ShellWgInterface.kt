package org.awgadmin.network

import java.io.BufferedReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.awgadmin.domain.ObfuscationParams
import org.awgadmin.domain.ServerConfig

/**
 * Shell-based implementation of WgInterface.
 * Uses awg/wg commands to interact with the interface.
 */
class ShellWgInterface(
    private val interfaceName: String,
    private val awgBinary: String = "awg",
    private val wgBinary: String = "wg",
    private val serverEndpoint: String,
    private val dns: List<String> = listOf("1.1.1.1", "8.8.8.8"),
) : WgInterface {

    override suspend fun getServerConfig(): ServerConfig {
        val output = executeCommand("$awgBinary show $interfaceName")
        val publicKey = extractValue(output, "public key")
        val listenPort = extractValue(output, "listening port").toIntOrNull() ?: 443

        return ServerConfig(
            interfaceName = interfaceName,
            publicKey = publicKey,
            listenPort = listenPort,
            endpoint = serverEndpoint,
            subnet = "10.66.66.0/24",
            dns = dns,
        )
    }

    override suspend fun getObfuscationParams(): ObfuscationParams {
        val output = executeCommand("$awgBinary show $interfaceName")

        return ObfuscationParams(
            jc = extractValue(output, "jc").toIntOrNull() ?: 0,
            jmin = extractValue(output, "jmin").toIntOrNull() ?: 0,
            jmax = extractValue(output, "jmax").toIntOrNull() ?: 0,
            s1 = extractValue(output, "s1").toIntOrNull() ?: 0,
            s2 = extractValue(output, "s2").toIntOrNull() ?: 0,
            h1 = extractValue(output, "h1").toLongOrNull() ?: 0L,
            h2 = extractValue(output, "h2").toLongOrNull() ?: 0L,
            h3 = extractValue(output, "h3").toLongOrNull() ?: 0L,
            h4 = extractValue(output, "h4").toLongOrNull() ?: 0L,
        )
    }

    override suspend fun addPeer(
        publicKey: String,
        presharedKey: String,
        allowedIps: String,
    ) {
        val tempPskFile = createTempFile(presharedKey)
        try {
            executeCommand(
                "$awgBinary set $interfaceName peer $publicKey " +
                    "preshared-key $tempPskFile allowed-ips $allowedIps"
            )
        } finally {
            tempPskFile.delete()
        }
    }

    override suspend fun removePeer(publicKey: String) {
        executeCommand("$awgBinary set $interfaceName peer $publicKey remove")
    }

    override suspend fun getPeersStatus(): List<PeerInfo> {
        val output = executeCommand("$awgBinary show $interfaceName dump")
        return parsePeersDump(output)
    }

    override suspend fun generateKeyPair(): KeyPair {
        val privateKey = executeCommand("$wgBinary genkey").trim()
        val publicKey = executeCommand("echo '$privateKey' | $wgBinary pubkey").trim()
        return KeyPair(privateKey = privateKey, publicKey = publicKey)
    }

    override suspend fun generatePresharedKey(): String {
        return executeCommand("$wgBinary genpsk").trim()
    }

    override suspend fun reload() {
        // For awg-go userspace, the interface stays active
        // Just sync the config if needed
    }

    private suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("bash", "-c", command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use(BufferedReader::readText)
        val exitCode = process.waitFor()

        if (exitCode != 0 && !output.contains("Warning")) {
            throw WgCommandException("Command failed with exit code $exitCode: $output")
        }
        output
    }

    private fun extractValue(output: String, key: String): String {
        val regex = Regex("""$key:\s*(.+)""", RegexOption.IGNORE_CASE)
        return regex.find(output)?.groupValues?.get(1)?.trim() ?: ""
    }

    private fun parsePeersDump(dump: String): List<PeerInfo> {
        // Format: public_key preshared_key endpoint allowed_ips latest_handshake transfer_rx transfer_tx
        return dump.lines()
            .drop(1) // Skip interface line
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size >= 7) {
                    PeerInfo(
                        publicKey = parts[0],
                        endpoint = parts[2].takeIf { it != "(none)" },
                        allowedIps = parts[3],
                        latestHandshake = parts[4].toLongOrNull()?.takeIf { it > 0 },
                        transferRx = parts[5].toLongOrNull() ?: 0L,
                        transferTx = parts[6].toLongOrNull() ?: 0L,
                    )
                } else {
                    null
                }
            }
    }

    private suspend fun createTempFile(content: String): java.io.File = withContext(Dispatchers.IO) {
        java.io.File.createTempFile("awg_psk_", ".key").apply {
            writeText(content)
            deleteOnExit()
        }
    }
}

/**
 * Exception for WireGuard command failures.
 */
class WgCommandException(message: String) : RuntimeException(message)
