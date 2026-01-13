package org.awgadmin.domain

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.awgadmin.data.ClientRepository
import org.awgadmin.network.WgInterface

/**
 * Service for managing VPN clients.
 * Coordinates between database and WireGuard interface.
 */
class ClientService(
    private val repository: ClientRepository,
    private val wgInterface: WgInterface,
) {

    suspend fun getAllClients(): List<Client> = repository.findAll()

    suspend fun getClient(id: String): Client? = repository.findById(id)

    suspend fun getClientsWithStatus(): List<Pair<Client, ClientStatus>> {
        val clients = repository.findAll()
        val peersStatus = wgInterface.getPeersStatus()

        return clients.map { client ->
            val peerInfo = peersStatus.find { it.publicKey == client.publicKey }
            val status = ClientStatus(
                clientId = client.id,
                isOnline = peerInfo?.let { isOnline(it.latestHandshake) } ?: false,
                lastHandshake = peerInfo?.latestHandshake?.let {
                    Instant.fromEpochSeconds(it)
                },
                transferRx = peerInfo?.transferRx ?: 0L,
                transferTx = peerInfo?.transferTx ?: 0L,
                endpoint = peerInfo?.endpoint,
            )
            client to status
        }
    }

    suspend fun createClient(request: CreateClientRequest): Client {
        val keyPair = wgInterface.generateKeyPair()
        val psk = wgInterface.generatePresharedKey()
        val ipAddress = repository.getNextAvailableIp()

        val client = repository.create(
            name = request.name,
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey,
            presharedKey = psk,
            ipAddress = ipAddress,
            expiresAt = request.expiresAt,
        )

        wgInterface.addPeer(
            publicKey = client.publicKey,
            presharedKey = client.presharedKey,
            allowedIps = "${client.ipAddress}/32",
        )

        return client
    }

    suspend fun updateClient(id: String, request: UpdateClientRequest): Client? {
        val client = repository.findById(id) ?: return null

        repository.update(
            id = id,
            name = request.name,
            expiresAt = request.expiresAt,
            isEnabled = request.isEnabled,
        )

        // Handle enable/disable on WG interface
        request.isEnabled?.let { enabled ->
            if (enabled && !client.isEnabled) {
                wgInterface.addPeer(
                    publicKey = client.publicKey,
                    presharedKey = client.presharedKey,
                    allowedIps = "${client.ipAddress}/32",
                )
            } else if (!enabled && client.isEnabled) {
                wgInterface.removePeer(client.publicKey)
            }
        }

        return repository.findById(id)
    }

    suspend fun deleteClient(id: String): Boolean {
        val client = repository.findById(id) ?: return false

        wgInterface.removePeer(client.publicKey)
        return repository.delete(id)
    }

    suspend fun getServerConfig(): ServerConfig = wgInterface.getServerConfig()

    suspend fun getObfuscationParams(): ObfuscationParams = wgInterface.getObfuscationParams()

    suspend fun generateClientConfig(clientId: String): String? {
        val client = repository.findById(clientId) ?: return null
        val serverConfig = wgInterface.getServerConfig()
        val obfuscation = wgInterface.getObfuscationParams()

        return buildClientConfig(client, serverConfig, obfuscation)
    }

    private fun buildClientConfig(
        client: Client,
        server: ServerConfig,
        obfuscation: ObfuscationParams,
    ): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${client.privateKey}")
        appendLine("Address = ${client.ipAddress}/32")
        appendLine("DNS = ${server.dns.joinToString(", ")}")
        appendLine()
        appendLine("Jc = ${obfuscation.jc}")
        appendLine("Jmin = ${obfuscation.jmin}")
        appendLine("Jmax = ${obfuscation.jmax}")
        appendLine("S1 = ${obfuscation.s1}")
        appendLine("S2 = ${obfuscation.s2}")
        appendLine("H1 = ${obfuscation.h1}")
        appendLine("H2 = ${obfuscation.h2}")
        appendLine("H3 = ${obfuscation.h3}")
        appendLine("H4 = ${obfuscation.h4}")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = ${server.publicKey}")
        appendLine("PresharedKey = ${client.presharedKey}")
        appendLine("Endpoint = ${server.endpoint}:${server.listenPort}")
        appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
        appendLine("PersistentKeepalive = 25")
    }

    private fun isOnline(lastHandshake: Long?): Boolean {
        if (lastHandshake == null || lastHandshake == 0L) return false
        val now = Clock.System.now().epochSeconds
        // Consider online if handshake was within last 3 minutes
        return (now - lastHandshake) < 180
    }
}
