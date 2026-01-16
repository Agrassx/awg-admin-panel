package org.awgadmin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.session
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionStorageMemory
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.sessions.clear
import io.ktor.server.request.receive
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.awgadmin.config.AppConfig
import org.awgadmin.data.ClientRepository
import org.awgadmin.data.DatabaseFactory
import org.awgadmin.data.UserRepository
import org.awgadmin.domain.AuthService
import org.awgadmin.domain.ClientService
import org.awgadmin.domain.RateLimitException
import org.awgadmin.domain.UserSession
import org.awgadmin.network.MockWgInterface
import org.awgadmin.network.ShellWgInterface
import org.awgadmin.network.WgCommandException
import org.awgadmin.network.WgInterface
import org.awgadmin.data.SettingsRepository
import org.awgadmin.domain.SettingsService
import org.awgadmin.routes.ErrorResponse
import org.awgadmin.routes.AuthResponse
import org.awgadmin.routes.LoginRequest
import org.awgadmin.routes.clientRoutes
import org.awgadmin.routes.settingsRoutes
import java.io.File
import java.security.SecureRandom

fun main() {
    val config = AppConfig.fromEnv()

    // Ensure data directory exists
    File(config.databasePath).parentFile?.mkdirs()

    // Initialize database
    DatabaseFactory.init(config.databasePath)

    // Initialize auth service and create default admin if needed
    val userRepository = UserRepository()
    val authService = AuthService(userRepository)

    // Create default admin user if none exists
    val generatedPassword = runBlocking {
        authService.initializeAdminUser()
    }

    if (generatedPassword != null) {
        println("=" .repeat(70))
        println("  FIRST RUN - Admin credentials generated")
        println("=" .repeat(70))
        println("  Username: admin")
        println("  Password: $generatedPassword")
        println("=" .repeat(70))
        println("  SAVE THIS PASSWORD! It will not be shown again.")
        println("  You can also find it in the container logs: docker logs <container>")
        println("=" .repeat(70))
    }

    // Create WireGuard interface abstraction
    val wgInterface: WgInterface = if (config.useMockWg) {
        println("[WARN] Using MOCK WireGuard interface (for development)")
        MockWgInterface(
            serverEndpoint = config.wgEndpoint,
            serverPort = config.wgPort,
            serverAddress = config.wgAddress,
            dns = config.dns,
            obfuscation = config.obfuscation,
        )
    } else {
        println("[INFO] Using Shell WireGuard interface: ${config.wgInterfaceName}")
        ShellWgInterface(
            interfaceName = config.wgInterfaceName,
            awgBinary = config.awgBinary,
            wgBinary = config.wgBinary,
            serverEndpoint = config.wgEndpoint,
            serverPort = config.wgPort,
            serverAddress = config.wgAddress,
            dns = config.dns,
            obfuscation = config.obfuscation,
        )
    }

    // Create services
    val clientRepository = ClientRepository()
    val settingsRepository = SettingsRepository()
    val clientService = ClientService(clientRepository, wgInterface)
    val settingsService = SettingsService(config, wgInterface, settingsRepository)

    println("[INFO] Starting AWG Admin on port ${config.serverPort}")

    // Security warning for production
    if (config.security.production && config.sessionSecret.isBlank()) {
        println("[WARN] SECURITY: SESSION_SECRET is not set! Sessions will be invalidated on restart.")
        println("[WARN] Generate one with: openssl rand -base64 32")
    }

    // Start server
    embeddedServer(
        Netty,
        port = config.serverPort,
        module = { configureApp(clientService, authService, settingsService, config) },
    ).start(wait = true)
}

fun Application.configureApp(
    clientService: ClientService,
    authService: AuthService,
    settingsService: SettingsService,
    config: AppConfig,
) {
    // Generate session signing key from secret
    val sessionSignKey = generateSessionKey(config.sessionSecret)
    val isProduction = config.security.production

    install(Sessions) {
        cookie<UserSession>("AWG_SESSION", SessionStorageMemory()) {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = 86400 // 24 hours
            cookie.extensions["SameSite"] = "Strict"
            // Enable Secure flag for HTTPS in production
            if (config.security.secureCookies) {
                cookie.secure = true
            }
            transform(
                io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication(
                    sessionSignKey,
                    "HmacSHA256"
                )
            )
        }
    }

    install(Authentication) {
        session<UserSession>("auth-session") {
            validate { session ->
                // Session is valid if it exists
                session
            }
            challenge {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Authentication required"))
            }
        }
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = !isProduction // Disable pretty print in production
                ignoreUnknownKeys = true
            }
        )
    }

    // Security headers
    install(DefaultHeaders) {
        header("X-Content-Type-Options", "nosniff")
        header("X-Frame-Options", "DENY")
        header("X-XSS-Protection", "1; mode=block")
        header("Referrer-Policy", "strict-origin-when-cross-origin")
        if (config.security.secureCookies) {
            header("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
        }
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
        
        // Security: Only allow specific origins in production
        if (config.security.allowedOrigins.isNotEmpty()) {
            config.security.allowedOrigins.forEach { origin ->
                allowHost(origin.removePrefix("https://").removePrefix("http://"))
            }
        } else {
            // Development mode: allow any host
            anyHost()
        }
    }

    install(StatusPages) {
        exception<RateLimitException> { call, cause ->
            call.respond(
                HttpStatusCode.TooManyRequests,
                ErrorResponse(cause.message ?: "Too many requests"),
            )
        }
        exception<WgCommandException> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("WireGuard error: ${cause.message}"),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Invalid request"),
            )
        }
        exception<Throwable> { call, cause ->
            if (!isProduction) {
                cause.printStackTrace()
            }
            call.respond(
                HttpStatusCode.InternalServerError,
                // Hide internal error details in production
                ErrorResponse(if (isProduction) "Internal server error" else "Internal error: ${cause.message}"),
            )
        }
    }

    routing {
        // Auth routes - inline definition to ensure they work
        route("/api/auth") {
            post("/login") {
                val request = call.receive<LoginRequest>()
                
                // Get client IP with security considerations
                val ipAddress = getClientIp(call, config.security.trustProxy)

                try {
                    val user = authService.authenticate(ipAddress, request.username, request.password)
                    if (user != null) {
                        call.sessions.set(UserSession(userId = user.id, username = user.username))
                        call.respond(AuthResponse(success = true, username = user.username, message = "Login successful"))
                    } else {
                        call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, username = null, message = "Invalid username or password"))
                    }
                } catch (e: RateLimitException) {
                    call.respond(HttpStatusCode.TooManyRequests, AuthResponse(success = false, username = null, message = e.message))
                }
            }

            post("/logout") {
                call.sessions.clear<UserSession>()
                call.respond(AuthResponse(success = true, username = null, message = "Logged out"))
            }

            get("/me") {
                val session = call.sessions.get<UserSession>()
                if (session != null) {
                    call.respond(AuthResponse(success = true, username = session.username, message = null))
                } else {
                    call.respond(HttpStatusCode.Unauthorized, AuthResponse(success = false, username = null, message = "Not authenticated"))
                }
            }

            post("/change-password") {
                val session = call.sessions.get<UserSession>()
                    ?: return@post call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthResponse(success = false, username = null, message = "Not authenticated")
                    )

                val request = call.receive<org.awgadmin.routes.ChangePasswordRequest>()

                try {
                    authService.changePassword(session.userId, request.newPassword)
                    call.respond(AuthResponse(success = true, username = session.username, message = "Password changed successfully"))
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        AuthResponse(success = false, username = null, message = e.message ?: "Invalid password")
                    )
                }
            }
        }

        // Protected API routes - require authentication
        authenticate("auth-session") {
            clientRoutes(clientService)
            settingsRoutes(settingsService)
        }

        // Static resources (frontend) - fallback for non-API routes
        staticResources("/", "static") {
            default("index.html")
        }
    }
}

/**
 * Generate a secure session signing key from a secret.
 */
private fun generateSessionKey(secret: String): ByteArray {
    // If no secret provided, generate a random one (will change on restart)
    if (secret.isBlank()) {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    // Derive key from secret using simple hash
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(secret.toByteArray())
}

/**
 * Get client IP address with proxy trust consideration.
 * 
 * Security: X-Forwarded-For can be spoofed. Only trust it when behind a known proxy.
 */
private fun getClientIp(call: io.ktor.server.application.ApplicationCall, trustProxy: Boolean): String {
    if (trustProxy) {
        // Try standard proxy headers
        val forwarded = call.request.headers["X-Forwarded-For"]
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { isValidIp(it) }
        
        if (forwarded != null) {
            return forwarded
        }
        
        // Try X-Real-IP (nginx)
        val realIp = call.request.headers["X-Real-IP"]
            ?.trim()
            ?.takeIf { isValidIp(it) }
        
        if (realIp != null) {
            return realIp
        }
    }
    
    // Fall back to direct connection IP
    return call.request.local.remoteAddress
}

/**
 * Basic IP address validation.
 */
private fun isValidIp(ip: String): Boolean {
    // IPv4
    val ipv4Regex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
    if (ipv4Regex.matches(ip)) {
        return ip.split(".").all { it.toIntOrNull() in 0..255 }
    }
    
    // IPv6 (simplified check)
    val ipv6Regex = """^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$""".toRegex()
    return ipv6Regex.matches(ip)
}
