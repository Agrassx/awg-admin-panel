package org.awgadmin.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.awgadmin.routes.NetworkSettingsDto
import org.awgadmin.routes.NetworkSettingsRequest
import org.awgadmin.routes.ObfuscationSettingsDto
import org.awgadmin.routes.ObfuscationSettingsRequest
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.upsert

/**
 * Repository for application settings.
 */
class SettingsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val KEY_OBFUSCATION = "obfuscation"
        private const val KEY_NETWORK = "network"
    }

    suspend fun getObfuscationSettings(): ObfuscationSettingsDto? = DatabaseFactory.dbQuery {
        SettingsTable.selectAll()
            .where { SettingsTable.key eq KEY_OBFUSCATION }
            .singleOrNull()
            ?.let { row ->
                try {
                    json.decodeFromString<ObfuscationSettingsDto>(row[SettingsTable.value])
                } catch (e: Exception) {
                    null
                }
            }
    }

    suspend fun saveObfuscationSettings(request: ObfuscationSettingsRequest) = DatabaseFactory.dbQuery {
        val current = SettingsTable.selectAll()
            .where { SettingsTable.key eq KEY_OBFUSCATION }
            .singleOrNull()
            ?.let { row ->
                try {
                    json.decodeFromString<ObfuscationSettingsDto>(row[SettingsTable.value])
                } catch (e: Exception) {
                    null
                }
            }

        val updated = ObfuscationSettingsDto(
            jc = request.jc ?: current?.jc ?: 4,
            jmin = request.jmin ?: current?.jmin ?: 40,
            jmax = request.jmax ?: current?.jmax ?: 70,
            s1 = request.s1 ?: current?.s1 ?: 55,
            s2 = request.s2 ?: current?.s2 ?: 55,
            h1 = request.h1 ?: current?.h1 ?: 1234567891,
            h2 = request.h2 ?: current?.h2 ?: 1234567892,
            h3 = request.h3 ?: current?.h3 ?: 1234567893,
            h4 = request.h4 ?: current?.h4 ?: 1234567894,
        )

        SettingsTable.upsert {
            it[key] = KEY_OBFUSCATION
            it[value] = json.encodeToString(updated)
        }
    }

    suspend fun getNetworkSettings(): NetworkSettingsDto? = DatabaseFactory.dbQuery {
        SettingsTable.selectAll()
            .where { SettingsTable.key eq KEY_NETWORK }
            .singleOrNull()
            ?.let { row ->
                try {
                    json.decodeFromString<NetworkSettingsDto>(row[SettingsTable.value])
                } catch (e: Exception) {
                    null
                }
            }
    }

    suspend fun saveNetworkSettings(request: NetworkSettingsRequest) = DatabaseFactory.dbQuery {
        val current = SettingsTable.selectAll()
            .where { SettingsTable.key eq KEY_NETWORK }
            .singleOrNull()
            ?.let { row ->
                try {
                    json.decodeFromString<NetworkSettingsDto>(row[SettingsTable.value])
                } catch (e: Exception) {
                    null
                }
            }

        val updated = NetworkSettingsDto(
            address = request.address ?: current?.address ?: "10.0.0.1/24",
            dns = request.dns ?: current?.dns ?: listOf("1.1.1.1", "8.8.8.8"),
            allowedIps = current?.allowedIps ?: "0.0.0.0/0",
        )

        SettingsTable.upsert {
            it[key] = KEY_NETWORK
            it[value] = json.encodeToString(updated)
        }
    }

    suspend fun getSetting(key: String): String? = DatabaseFactory.dbQuery {
        SettingsTable.selectAll()
            .where { SettingsTable.key eq key }
            .singleOrNull()
            ?.get(SettingsTable.value)
    }

    suspend fun setSetting(key: String, value: String) = DatabaseFactory.dbQuery {
        SettingsTable.upsert {
            it[SettingsTable.key] = key
            it[SettingsTable.value] = value
        }
    }
}
