package org.awgadmin.api

import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
    val id: String,
    val name: String,
    val ipAddress: String,
    val expiresAt: String? = null,
    val isEnabled: Boolean,
    val createdAt: String,
    val isOnline: Boolean = false,
    val lastHandshake: String? = null,
    val transferRx: Long = 0,
    val transferTx: Long = 0,
)

@Serializable
data class CreateClientRequest(
    val name: String,
    val expiresAt: String?,
)

@Serializable
data class UpdateClientRequest(
    val name: String?,
    val expiresAt: String?,
    val isEnabled: Boolean?,
)

@Serializable
data class ServerConfigDto(
    val interfaceName: String,
    val publicKey: String,
    val listenPort: Int,
    val endpoint: String,
    val subnet: String,
    val dns: List<String>,
)

// Auth models
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val username: String? = null,
    val message: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    val newPassword: String,
)

// Settings models
@Serializable
data class SettingsResponse(
    val server: ServerSettings,
    val obfuscation: ObfuscationSettings,
    val network: NetworkSettings,
)

@Serializable
data class ServerSettings(
    val interfaceName: String,
    val publicKey: String,
    val endpoint: String,
    val listenPort: Int,
)

@Serializable
data class ObfuscationSettings(
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
data class NetworkSettings(
    val address: String,
    val dns: List<String>,
    val allowedIps: String,
)

@Serializable
data class UpdateObfuscationRequest(
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
data class UpdateNetworkRequest(
    val address: String? = null,
    val dns: List<String>? = null,
)

@Serializable
data class ServerStatus(
    val isRunning: Boolean,
    val interfaceName: String,
    val uptime: String? = null,
    val peersCount: Int,
    val transferRx: Long,
    val transferTx: Long,
    val lastHandshake: String? = null,
)

@Serializable
data class SuccessResponse(
    val message: String,
)
