package org.awgadmin.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.awgadmin.data.DatabaseFactory.dbQuery
import org.awgadmin.domain.Client
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/**
 * Repository for client data access.
 */
class ClientRepository {

    suspend fun findAll(): List<Client> = dbQuery {
        ClientTable.selectAll().map(::toClient)
    }

    suspend fun findById(id: String): Client? = dbQuery {
        ClientTable.selectAll()
            .where { ClientTable.id eq id }
            .map(::toClient)
            .singleOrNull()
    }

    suspend fun findByPublicKey(publicKey: String): Client? = dbQuery {
        ClientTable.selectAll()
            .where { ClientTable.publicKey eq publicKey }
            .map(::toClient)
            .singleOrNull()
    }

    suspend fun create(
        name: String,
        publicKey: String,
        privateKey: String,
        presharedKey: String,
        ipAddress: String,
        expiresAt: Instant?,
    ): Client = dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()

        ClientTable.insert {
            it[ClientTable.id] = id
            it[ClientTable.name] = name
            it[ClientTable.publicKey] = publicKey
            it[ClientTable.privateKey] = privateKey
            it[ClientTable.presharedKey] = presharedKey
            it[ClientTable.ipAddress] = ipAddress
            it[ClientTable.expiresAt] = expiresAt
            it[isEnabled] = true
            it[createdAt] = now
        }

        Client(
            id = id,
            name = name,
            publicKey = publicKey,
            privateKey = privateKey,
            presharedKey = presharedKey,
            ipAddress = ipAddress,
            expiresAt = expiresAt,
            isEnabled = true,
            createdAt = now,
        )
    }

    suspend fun update(
        id: String,
        name: String?,
        expiresAt: Instant?,
        isEnabled: Boolean?,
    ): Boolean = dbQuery {
        ClientTable.update({ ClientTable.id eq id }) { row ->
            name?.let { row[ClientTable.name] = it }
            expiresAt?.let { row[ClientTable.expiresAt] = it }
            isEnabled?.let { row[ClientTable.isEnabled] = it }
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        ClientTable.deleteWhere { ClientTable.id eq id } > 0
    }

    suspend fun getNextAvailableIp(subnet: String = "10.66.66"): String = dbQuery {
        val usedIps = ClientTable.selectAll()
            .map { it[ClientTable.ipAddress] }
            .mapNotNull { ip ->
                ip.split(".").lastOrNull()?.toIntOrNull()
            }
            .toSet()

        // Start from .2 (server is .1)
        val nextOctet = (2..254).first { it !in usedIps }
        "$subnet.$nextOctet"
    }

    private fun toClient(row: ResultRow): Client = Client(
        id = row[ClientTable.id],
        name = row[ClientTable.name],
        publicKey = row[ClientTable.publicKey],
        privateKey = row[ClientTable.privateKey],
        presharedKey = row[ClientTable.presharedKey],
        ipAddress = row[ClientTable.ipAddress],
        expiresAt = row[ClientTable.expiresAt],
        isEnabled = row[ClientTable.isEnabled],
        createdAt = row[ClientTable.createdAt],
    )
}
