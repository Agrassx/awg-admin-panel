package org.awgadmin

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.awgadmin.config.AppConfig
import org.awgadmin.data.ClientRepository
import org.awgadmin.data.DatabaseFactory
import org.awgadmin.domain.ClientService
import org.awgadmin.network.MockWgInterface
import org.awgadmin.network.ShellWgInterface
import org.awgadmin.network.WgCommandException
import org.awgadmin.network.WgInterface
import org.awgadmin.routes.ErrorResponse
import org.awgadmin.routes.clientRoutes
import java.io.File

fun main() {
    val config = AppConfig.fromEnv()

    // Ensure data directory exists
    File(config.databasePath).parentFile?.mkdirs()

    // Initialize database
    DatabaseFactory.init(config.databasePath)

    // Create WireGuard interface abstraction
    val wgInterface: WgInterface = if (config.useMockWg) {
        println("[WARN] Using MOCK WireGuard interface (for development)")
        MockWgInterface(
            serverEndpoint = config.wgEndpoint,
            dns = config.dns,
        )
    } else {
        println("[INFO] Using Shell WireGuard interface: ${config.wgInterfaceName}")
        ShellWgInterface(
            interfaceName = config.wgInterfaceName,
            awgBinary = config.awgBinary,
            wgBinary = config.wgBinary,
            serverEndpoint = config.wgEndpoint,
            dns = config.dns,
        )
    }

    // Create services
    val clientRepository = ClientRepository()
    val clientService = ClientService(clientRepository, wgInterface)

    println("[INFO] Starting AWG Admin on port ${config.serverPort}")

    // Start server
    embeddedServer(
        Netty,
        port = config.serverPort,
        module = { configureApp(clientService) },
    ).start(wait = true)
}

fun Application.configureApp(clientService: ClientService) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        anyHost()
    }

    install(StatusPages) {
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
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal error: ${cause.message}"),
            )
        }
    }

    routing {
        clientRoutes(clientService)
        staticResources("/", "static") {
            default("index.html")
        }
    }
}
