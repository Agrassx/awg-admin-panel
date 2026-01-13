package org.awgadmin.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a VPN client with its configuration and status.
 */
@Serializable
data class Client(
    val id: String,
    val name: String,
    val publicKey: String,
    val privateKey: String,
    val presharedKey: String,
    val ipAddress: String,
    val expiresAt: Instant?,
    val isEnabled: Boolean,
    val createdAt: Instant,
)

/**
 * Client connection status.
 */
@Serializable
data class ClientStatus(
    val clientId: String,
    val isOnline: Boolean,
    val lastHandshake: Instant?,
    val transferRx: Long,
    val transferTx: Long,
    val endpoint: String?,
)

/**
 * Request to create a new client.
 */
@Serializable
data class CreateClientRequest(
    val name: String,
    val expiresAt: Instant?,
)

/**
 * Request to update client settings.
 */
@Serializable
data class UpdateClientRequest(
    val name: String?,
    val expiresAt: Instant?,
    val isEnabled: Boolean?,
)
