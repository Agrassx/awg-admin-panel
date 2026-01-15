package org.awgadmin.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Domain model for an admin user.
 */
data class User(
    val id: String,
    val username: String,
    val passwordHash: String,
    val createdAt: Instant,
    val lastLoginAt: Instant?,
)

/**
 * User session data stored in cookie.
 */
@Serializable
data class UserSession(
    val userId: String,
    val username: String,
)
