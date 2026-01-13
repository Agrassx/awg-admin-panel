package org.awgadmin.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Database table for storing VPN clients.
 */
object ClientTable : Table("clients") {
    val id = varchar("id", 36)
    val name = varchar("name", 255)
    val publicKey = varchar("public_key", 64)
    val privateKey = varchar("private_key", 64)
    val presharedKey = varchar("preshared_key", 64)
    val ipAddress = varchar("ip_address", 18)
    val expiresAt = timestamp("expires_at").nullable()
    val isEnabled = bool("is_enabled").default(true)
    val createdAt = timestamp("created_at")

    override val primaryKey: PrimaryKey = PrimaryKey(id)
}
