package org.awgadmin.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Database table for admin users.
 */
object UserTable : Table("users") {
    val id = varchar("id", 36)
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val createdAt = timestamp("created_at")
    val lastLoginAt = timestamp("last_login_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Table for tracking failed login attempts (brute-force protection).
 */
object LoginAttemptTable : Table("login_attempts") {
    val id = integer("id").autoIncrement()
    val ipAddress = varchar("ip_address", 45)
    val username = varchar("username", 100)
    val attemptedAt = timestamp("attempted_at")
    val successful = bool("successful")

    override val primaryKey = PrimaryKey(id)
}
