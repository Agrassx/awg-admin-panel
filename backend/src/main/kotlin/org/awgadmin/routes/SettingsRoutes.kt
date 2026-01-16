package org.awgadmin.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.awgadmin.domain.SettingsService

/**
 * Settings API routes.
 */
fun Route.settingsRoutes(settingsService: SettingsService) {
    route("/api/settings") {
        // Get all settings
        get {
            val settings = settingsService.getSettings()
            call.respond(settings)
        }

        // Update AWG obfuscation settings
        patch("/obfuscation") {
            val request = call.receive<ObfuscationSettingsRequest>()
            try {
                settingsService.updateObfuscation(request)
                call.respond(SuccessResponse("Obfuscation settings updated. Restart required for changes to take effect."))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid settings"))
            }
        }

        // Update network settings
        patch("/network") {
            val request = call.receive<NetworkSettingsRequest>()
            try {
                settingsService.updateNetwork(request)
                call.respond(SuccessResponse("Network settings updated. Restart required for changes to take effect."))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid settings"))
            }
        }

        // Get server status
        get("/status") {
            val status = settingsService.getServerStatus()
            call.respond(status)
        }

        // Restart WireGuard interface
        // post("/restart") {
        //     settingsService.restartInterface()
        //     call.respond(SuccessResponse("WireGuard interface restarted"))
        // }
    }
}

@Serializable
data class SuccessResponse(val message: String)

@Serializable
data class SettingsResponse(
    val server: ServerSettingsDto,
    val obfuscation: ObfuscationSettingsDto,
    val network: NetworkSettingsDto,
)

@Serializable
data class ServerSettingsDto(
    val interfaceName: String,
    val publicKey: String,
    val endpoint: String,
    val listenPort: Int,
)

@Serializable
data class ObfuscationSettingsDto(
    val jc: Int,
    val jmin: Int,
    val jmax: Int,
    val s1: Int,
    val s2: Int,
    val h1: Long,
    val h2: Long,
    val h3: Long,
    val h4: Long,
)

@Serializable
data class NetworkSettingsDto(
    val address: String,
    val dns: List<String>,
    val allowedIps: String,
)

@Serializable
data class ObfuscationSettingsRequest(
    val jc: Int? = null,
    val jmin: Int? = null,
    val jmax: Int? = null,
    val s1: Int? = null,
    val s2: Int? = null,
    val h1: Long? = null,
    val h2: Long? = null,
    val h3: Long? = null,
    val h4: Long? = null,
)

@Serializable
data class NetworkSettingsRequest(
    val address: String? = null,
    val dns: List<String>? = null,
)

@Serializable
data class ServerStatusDto(
    val isRunning: Boolean,
    val interfaceName: String,
    val uptime: String?,
    val peersCount: Int,
    val transferRx: Long,
    val transferTx: Long,
    val lastHandshake: String?,
)
