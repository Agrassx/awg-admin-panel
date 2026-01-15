package org.awgadmin.data

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Database configuration and initialization.
 */
object DatabaseFactory {

    fun init(dbPath: String = "./data/awg-admin.db") {
        val driver = "org.sqlite.JDBC"
        val url = "jdbc:sqlite:$dbPath"

        Database.connect(url, driver)

        transaction {
            SchemaUtils.create(
                ClientTable,
                UserTable,
                LoginAttemptTable,
            )
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
