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
