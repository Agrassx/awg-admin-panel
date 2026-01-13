package org.awgadmin.config

/**
 * Application configuration loaded from environment variables.
 */
data class AppConfig(
    val serverPort: Int,
    val wgInterfaceName: String,
    val wgEndpoint: String,
    val databasePath: String,
    val awgBinary: String,
    val wgBinary: String,
    val dns: List<String>,
    val useMockWg: Boolean,
) {
    companion object {
        fun fromEnv(): AppConfig = AppConfig(
            serverPort = env("SERVER_PORT", "8080").toInt(),
            wgInterfaceName = env("WG_INTERFACE", "awg0"),
            wgEndpoint = env("WG_ENDPOINT", "localhost"),
            databasePath = env("DATABASE_PATH", "./data/awg-admin.db"),
            awgBinary = env("AWG_BINARY", "awg"),
            wgBinary = env("WG_BINARY", "wg"),
            dns = env("DNS_SERVERS", "1.1.1.1,8.8.8.8").split(","),
            useMockWg = env("USE_MOCK_WG", "false").toBoolean(),
        )

        private fun env(name: String, default: String): String =
            System.getenv(name) ?: default
    }
}
