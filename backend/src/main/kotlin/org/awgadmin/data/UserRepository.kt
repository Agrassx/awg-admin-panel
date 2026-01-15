package org.awgadmin.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.awgadmin.domain.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Repository for user data access operations.
 */
class UserRepository {

    suspend fun findByUsername(username: String): User? = DatabaseFactory.dbQuery {
        UserTable.selectAll()
            .where { UserTable.username eq username }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun findById(id: String): User? = DatabaseFactory.dbQuery {
        UserTable.selectAll()
            .where { UserTable.id eq id }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun create(username: String, passwordHash: String): User = DatabaseFactory.dbQuery {
        val id = UUID.randomUUID().toString()
        val now = Clock.System.now()

        UserTable.insert {
            it[UserTable.id] = id
            it[UserTable.username] = username
            it[UserTable.passwordHash] = passwordHash
            it[createdAt] = now
            it[lastLoginAt] = null
        }

        User(
            id = id,
            username = username,
            passwordHash = passwordHash,
            createdAt = now,
            lastLoginAt = null,
        )
    }

    suspend fun updatePassword(id: String, passwordHash: String): Boolean = DatabaseFactory.dbQuery {
        UserTable.update({ UserTable.id eq id }) {
            it[UserTable.passwordHash] = passwordHash
        } > 0
    }

    suspend fun updateLastLogin(id: String): Boolean = DatabaseFactory.dbQuery {
        UserTable.update({ UserTable.id eq id }) {
            it[lastLoginAt] = Clock.System.now()
        } > 0
    }

    suspend fun count(): Long = DatabaseFactory.dbQuery {
        UserTable.selectAll().count()
    }

    suspend fun getAll(): List<User> = DatabaseFactory.dbQuery {
        UserTable.selectAll().map { it.toUser() }
    }

    suspend fun delete(id: String): Boolean = DatabaseFactory.dbQuery {
        UserTable.deleteWhere { UserTable.id eq id } > 0
    }

    /**
     * Records a login attempt for brute-force protection.
     */
    suspend fun recordLoginAttempt(
        ipAddress: String,
        username: String,
        successful: Boolean,
    ) = DatabaseFactory.dbQuery {
        LoginAttemptTable.insert {
            it[LoginAttemptTable.ipAddress] = ipAddress
            it[LoginAttemptTable.username] = username
            it[attemptedAt] = Clock.System.now()
            it[LoginAttemptTable.successful] = successful
        }
    }

    /**
     * Counts failed login attempts from an IP in the last N minutes.
     */
    suspend fun countRecentFailedAttempts(
        ipAddress: String,
        windowMinutes: Int = 15,
    ): Long = DatabaseFactory.dbQuery {
        val cutoff = Clock.System.now() - windowMinutes.minutes

        LoginAttemptTable.selectAll()
            .where {
                (LoginAttemptTable.ipAddress eq ipAddress) and
                    (LoginAttemptTable.attemptedAt greaterEq cutoff) and
                    (LoginAttemptTable.successful eq false)
            }
            .count()
    }

    /**
     * Cleans up old login attempts (older than 24 hours).
     */
    suspend fun cleanupOldAttempts() = DatabaseFactory.dbQuery {
        val cutoff = Clock.System.now() - 1440.minutes // 24 hours

        LoginAttemptTable.deleteWhere {
            LoginAttemptTable.attemptedAt less cutoff
        }
    }

    private fun ResultRow.toUser() = User(
        id = this[UserTable.id],
        username = this[UserTable.username],
        passwordHash = this[UserTable.passwordHash],
        createdAt = this[UserTable.createdAt],
        lastLoginAt = this[UserTable.lastLoginAt],
    )
}
