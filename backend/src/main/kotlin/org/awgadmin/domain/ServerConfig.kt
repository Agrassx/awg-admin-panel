package org.awgadmin.domain

import kotlinx.serialization.Serializable

/**
 * Server configuration for the VPN interface.
 */
@Serializable
data class ServerConfig(
    val interfaceName: String,
    val publicKey: String,
    val listenPort: Int,
    val endpoint: String,
    val subnet: String,
    val dns: List<String>,
)

/**
 * AmneziaWG obfuscation parameters.
 */
@Serializable
data class ObfuscationParams(
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
