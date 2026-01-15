package org.awgadmin.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json

object ApiClient {
    private val baseUrl: String
        get() = window.location.origin

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    // Auth methods
    suspend fun login(username: String, password: String): AuthResponse {
        val response: HttpResponse = client.post("$baseUrl/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }
        return response.body()
    }

    suspend fun logout(): AuthResponse =
        client.post("$baseUrl/api/auth/logout").body()

    suspend fun checkAuth(): AuthResponse? {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/auth/me")
            if (response.status.isSuccess()) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun changePassword(newPassword: String): AuthResponse =
        client.post("$baseUrl/api/auth/change-password") {
            contentType(ContentType.Application.Json)
            setBody(ChangePasswordRequest(newPassword))
        }.body()

    // Client methods
    suspend fun getClients(): List<ClientDto> =
        client.get("$baseUrl/api/clients").body()

    suspend fun createClient(request: CreateClientRequest): ClientDto =
        client.post("$baseUrl/api/clients") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun updateClient(id: String, request: UpdateClientRequest): ClientDto =
        client.patch("$baseUrl/api/clients/$id") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    suspend fun deleteClient(id: String) {
        client.delete("$baseUrl/api/clients/$id")
    }

    suspend fun toggleClient(id: String): ClientDto =
        client.post("$baseUrl/api/clients/$id/toggle").body()

    suspend fun getClientConfig(id: String): String =
        client.get("$baseUrl/api/clients/$id/config").bodyAsText()

    suspend fun getServerConfig(): ServerConfigDto =
        client.get("$baseUrl/api/server/config").body()
}
