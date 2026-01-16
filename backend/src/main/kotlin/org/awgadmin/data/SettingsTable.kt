package org.awgadmin.data

import org.jetbrains.exposed.sql.Table

/**
 * Table for storing application settings as key-value pairs.
 */
object SettingsTable : Table("settings") {
    val key = varchar("key", 100)
    val value = text("value")

    override val primaryKey = PrimaryKey(key)
}
