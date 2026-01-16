package org.awgadmin.config

/**
 * Application configuration loaded from environment variables.
 */
data class AppConfig(
    val serverPort: Int,
    val wgInterfaceName: String,
    val wgEndpoint: String,
    val wgPort: Int,
    val wgAddress: String,
    val databasePath: String,
    val awgBinary: String,
    val wgBinary: String,
    val dns: List<String>,
    val useMockWg: Boolean,
    val sessionSecret: String,
    val obfuscation: ObfuscationConfig,
    val security: SecurityConfig,
) {
    companion object {
        fun fromEnv(): AppConfig = AppConfig(
            serverPort = env("SERVER_PORT", "8080").toInt(),
            wgInterfaceName = env("WG_INTERFACE", "awg0"),
            wgEndpoint = env("WG_ENDPOINT", "localhost"),
            wgPort = env("WG_PORT", "51820").toInt(),
            wgAddress = env("WG_ADDRESS", "10.0.0.1/24"),
            databasePath = env("DATABASE_PATH", "./data/awg-admin.db"),
            awgBinary = env("AWG_BINARY", "awg"),
            wgBinary = env("WG_BINARY", "wg"),
            dns = env("DNS_SERVERS", "1.1.1.1,8.8.8.8").split(","),
            useMockWg = env("USE_MOCK_WG", "false").toBoolean(),
            sessionSecret = env("SESSION_SECRET", ""),
            obfuscation = ObfuscationConfig.fromEnv(),
            security = SecurityConfig.fromEnv(),
        )

        private fun env(name: String, default: String): String =
            System.getenv(name) ?: default
    }
}

/**
 * Security configuration.
 */
data class SecurityConfig(
    val production: Boolean,        // Enable production security settings
    val secureCookies: Boolean,     // Use Secure flag on cookies (HTTPS only)
    val allowedOrigins: List<String>, // CORS allowed origins (empty = allow configured origins only)
    val trustProxy: Boolean,        // Trust X-Forwarded-For header
) {
    companion object {
        fun fromEnv(): SecurityConfig = SecurityConfig(
            production = env("PRODUCTION", "false").toBoolean(),
            secureCookies = env("SECURE_COOKIES", "false").toBoolean(),
            allowedOrigins = env("ALLOWED_ORIGINS", "").split(",").filter { it.isNotBlank() },
            trustProxy = env("TRUST_PROXY", "true").toBoolean(),
        )

        private fun env(name: String, default: String): String =
            System.getenv(name) ?: default
    }
}

/**
 * AmneziaWG obfuscation settings.
 * These must match between server and all clients.
 */
data class ObfuscationConfig(
    val jc: Int,    // Junk packet count (1-128)
    val jmin: Int,  // Junk packet min size (0-1280)
    val jmax: Int,  // Junk packet max size (0-1280)
    val s1: Int,    // Init packet first part size
    val s2: Int,    // Init packet second part size
    val h1: Long,   // Header obfuscation key 1
    val h2: Long,   // Header obfuscation key 2
    val h3: Long,   // Header obfuscation key 3
    val h4: Long,   // Header obfuscation key 4
) {
    companion object {
        fun fromEnv(): ObfuscationConfig = ObfuscationConfig(
            jc = env("AWG_JC", "4").toInt(),
            jmin = env("AWG_JMIN", "40").toInt(),
            jmax = env("AWG_JMAX", "70").toInt(),
            s1 = env("AWG_S1", "55").toInt(),
            s2 = env("AWG_S2", "55").toInt(),
            h1 = env("AWG_H1", "1234567891").toLong(),
            h2 = env("AWG_H2", "1234567892").toLong(),
            h3 = env("AWG_H3", "1234567893").toLong(),
            h4 = env("AWG_H4", "1234567894").toLong(),
        )

        private fun env(name: String, default: String): String =
            System.getenv(name) ?: default
    }
}
