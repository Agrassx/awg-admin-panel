package org.awgadmin.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import org.awgadmin.domain.ClientService
import org.awgadmin.domain.ClientStatus
import org.awgadmin.domain.CreateClientRequest
import org.awgadmin.domain.UpdateClientRequest

/**
 * REST API routes for client management.
 */
fun Route.clientRoutes(clientService: ClientService) {
    route("/api/clients") {

        get {
            val clientsWithStatus = clientService.getClientsWithStatus()
            val response = clientsWithStatus.map { (client, status) ->
                ClientResponse(
                    id = client.id,
                    name = client.name,
                    ipAddress = client.ipAddress,
                    expiresAt = client.expiresAt?.toString(),
                    isEnabled = client.isEnabled,
                    createdAt = client.createdAt.toString(),
                    isOnline = status.isOnline,
                    lastHandshake = status.lastHandshake?.toString(),
                    transferRx = status.transferRx,
                    transferTx = status.transferTx,
                )
            }
            call.respond(response)
        }

        post {
            val request = call.receive<CreateClientRequest>()
            val client = clientService.createClient(request)
            call.respond(HttpStatusCode.Created, client)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing id"),
            )
            val client = clientService.getClient(id) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Client not found"),
            )
            call.respond(client)
        }

        patch("/{id}") {
            val id = call.parameters["id"] ?: return@patch call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing id"),
            )
            val request = call.receive<UpdateClientRequest>()
            val client = clientService.updateClient(id, request) ?: return@patch call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Client not found"),
            )
            call.respond(client)
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing id"),
            )
            val deleted = clientService.deleteClient(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Client not found"))
            }
        }

        get("/{id}/config") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing id"),
            )
            val config = clientService.generateClientConfig(id) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Client not found"),
            )
            call.respondText(config, ContentType.Text.Plain)
        }

        post("/{id}/toggle") {
            val id = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("Missing id"),
            )
            val client = clientService.getClient(id) ?: return@post call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse("Client not found"),
            )
            val updated = clientService.updateClient(
                id = id,
                request = UpdateClientRequest(
                    name = null,
                    expiresAt = null,
                    isEnabled = !client.isEnabled,
                ),
            )
            call.respond(updated ?: client)
        }
    }

    route("/api/server") {
        get("/config") {
            val config = clientService.getServerConfig()
            call.respond(config)
        }

        get("/obfuscation") {
            val params = clientService.getObfuscationParams()
            call.respond(params)
        }
    }
}

@Serializable
data class ClientResponse(
    val id: String,
    val name: String,
    val ipAddress: String,
    val expiresAt: String?,
    val isEnabled: Boolean,
    val createdAt: String,
    val isOnline: Boolean,
    val lastHandshake: String?,
    val transferRx: Long,
    val transferTx: Long,
)

@Serializable
data class ErrorResponse(val message: String)
