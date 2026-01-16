package org.awgadmin.domain

import org.awgadmin.config.AppConfig
import org.awgadmin.data.SettingsRepository
import org.awgadmin.network.WgInterface
import org.awgadmin.routes.NetworkSettingsDto
import org.awgadmin.routes.NetworkSettingsRequest
import org.awgadmin.routes.ObfuscationSettingsDto
import org.awgadmin.routes.ObfuscationSettingsRequest
import org.awgadmin.routes.ServerSettingsDto
import org.awgadmin.routes.ServerStatusDto
import org.awgadmin.routes.SettingsResponse

/**
 * Service for managing server settings.
 */
class SettingsService(
    private val config: AppConfig,
    private val wgInterface: WgInterface,
    private val settingsRepository: SettingsRepository,
) {
    /**
     * Get all current settings.
     */
    suspend fun getSettings(): SettingsResponse {
        val serverConfig = wgInterface.getServerConfig()
        val savedObfuscation = settingsRepository.getObfuscationSettings()
        val savedNetwork = settingsRepository.getNetworkSettings()

        return SettingsResponse(
            server = ServerSettingsDto(
                interfaceName = serverConfig.interfaceName,
                publicKey = serverConfig.publicKey,
                endpoint = serverConfig.endpoint,
                listenPort = serverConfig.listenPort,
            ),
            obfuscation = savedObfuscation ?: ObfuscationSettingsDto(
                jc = config.obfuscation.jc,
                jmin = config.obfuscation.jmin,
                jmax = config.obfuscation.jmax,
                s1 = config.obfuscation.s1,
                s2 = config.obfuscation.s2,
                h1 = config.obfuscation.h1,
                h2 = config.obfuscation.h2,
                h3 = config.obfuscation.h3,
                h4 = config.obfuscation.h4,
            ),
            network = savedNetwork ?: NetworkSettingsDto(
                address = config.wgAddress,
                dns = config.dns,
                allowedIps = "0.0.0.0/0",
            ),
        )
    }

    /**
     * Update obfuscation settings.
     */
    suspend fun updateObfuscation(request: ObfuscationSettingsRequest) {
        // Validate settings
        request.jc?.let {
            require(it in 1..128) { "Jc must be between 1 and 128" }
        }
        request.jmin?.let {
            require(it in 0..1280) { "Jmin must be between 0 and 1280" }
        }
        request.jmax?.let { jmax ->
            val jmin = request.jmin ?: settingsRepository.getObfuscationSettings()?.jmin ?: config.obfuscation.jmin
            require(jmax in 0..1280) { "Jmax must be between 0 and 1280" }
            require(jmax >= jmin) { "Jmax must be >= Jmin" }
        }
        request.s1?.let {
            require(it in 0..255) { "S1 must be between 0 and 255" }
        }
        request.s2?.let {
            require(it in 0..255) { "S2 must be between 0 and 255" }
        }

        settingsRepository.saveObfuscationSettings(request)
    }

    /**
     * Update network settings.
     */
    suspend fun updateNetwork(request: NetworkSettingsRequest) {
        // Validate address format
        request.address?.let { addr ->
            require(addr.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/\d{1,2}$"""))) {
                "Address must be in CIDR format (e.g., 10.0.0.1/24)"
            }
        }

        // Validate DNS
        request.dns?.let { dnsList ->
            require(dnsList.isNotEmpty()) { "At least one DNS server is required" }
            dnsList.forEach { dns ->
                require(dns.matches(Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$"""))) {
                    "Invalid DNS server format: $dns"
                }
            }
        }

        settingsRepository.saveNetworkSettings(request)
    }

    /**
     * Get server status.
     */
    suspend fun getServerStatus(): ServerStatusDto {
        return try {
            val serverConfig = wgInterface.getServerConfig()
            val peers = wgInterface.getPeersStatus()

            var totalRx = 0L
            var totalTx = 0L
            var lastHandshakeTime: Long? = null

            peers.forEach { peer ->
                totalRx += peer.transferRx
                totalTx += peer.transferTx
                peer.latestHandshake?.let { handshake ->
                    if (lastHandshakeTime == null || handshake > lastHandshakeTime!!) {
                        lastHandshakeTime = handshake
                    }
                }
            }

            ServerStatusDto(
                isRunning = true,
                interfaceName = serverConfig.interfaceName,
                uptime = null, // Would need to track start time
                peersCount = peers.size,
                transferRx = totalRx,
                transferTx = totalTx,
                lastHandshake = lastHandshakeTime?.let { 
                    kotlinx.datetime.Instant.fromEpochSeconds(it).toString() 
                },
            )
        } catch (e: Exception) {
            ServerStatusDto(
                isRunning = false,
                interfaceName = config.wgInterfaceName,
                uptime = null,
                peersCount = 0,
                transferRx = 0,
                transferTx = 0,
                lastHandshake = null,
            )
        }
    }
}
