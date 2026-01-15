package org.awgadmin.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.Serializable
import org.awgadmin.domain.AuthService
import org.awgadmin.domain.RateLimitException
import org.awgadmin.domain.UserSession

/**
 * Authentication API routes.
 */
fun Route.authRoutes(authService: AuthService) {

    route("/api/auth") {

        /**
         * Login endpoint.
         * POST /api/auth/login
         */
        post("/login") {
            val request = call.receive<LoginRequest>()

            // Get client IP for rate limiting
            val ipAddress = call.request.headers["X-Forwarded-For"]
                ?.split(",")
                ?.firstOrNull()
                ?.trim()
                ?: call.request.local.remoteAddress

            try {
                val user = authService.authenticate(
                    ipAddress = ipAddress,
                    username = request.username,
                    password = request.password,
                )

                if (user != null) {
                    call.sessions.set(UserSession(userId = user.id, username = user.username))
                    call.respond(
                        AuthResponse(
                            success = true,
                            username = user.username,
                            message = "Login successful",
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthResponse(
                            success = false,
                            username = null,
                            message = "Invalid username or password",
                        ),
                    )
                }
            } catch (e: RateLimitException) {
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    AuthResponse(
                        success = false,
                        username = null,
                        message = e.message ?: "Too many attempts",
                    ),
                )
            }
        }

        /**
         * Logout endpoint.
         * POST /api/auth/logout
         */
        post("/logout") {
            call.sessions.clear<UserSession>()
            call.respond(AuthResponse(success = true, username = null, message = "Logged out"))
        }

        /**
         * Check current session.
         * GET /api/auth/me
         */
        get("/me") {
            val session = call.sessions.get<UserSession>()

            if (session != null) {
                call.respond(
                    AuthResponse(
                        success = true,
                        username = session.username,
                        message = null,
                    )
                )
            } else {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse(
                        success = false,
                        username = null,
                        message = "Not authenticated",
                    ),
                )
            }
        }

        /**
         * Change password for current user.
         * POST /api/auth/change-password
         */
        post("/change-password") {
            val session = call.sessions.get<UserSession>()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse(success = false, username = null, message = "Not authenticated"),
                )

            val request = call.receive<ChangePasswordRequest>()

            try {
                authService.changePassword(session.userId, request.newPassword)
                call.respond(
                    AuthResponse(
                        success = true,
                        username = session.username,
                        message = "Password changed successfully",
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    AuthResponse(
                        success = false,
                        username = null,
                        message = e.message ?: "Invalid password",
                    ),
                )
            }
        }
    }

    // Admin user management routes (protected)
    route("/api/users") {

        get {
            val session = call.sessions.get<UserSession>()
                ?: return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Not authenticated"))

            val users = authService.getAllUsers()
            call.respond(users.map { user ->
                UserResponse(
                    id = user.id,
                    username = user.username,
                    createdAt = user.createdAt,
                    lastLoginAt = user.lastLoginAt,
                )
            })
        }

        post {
            val session = call.sessions.get<UserSession>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Not authenticated"))

            val request = call.receive<CreateUserRequest>()

            try {
                val (userInfo, password) = authService.createUser(request.username)
                call.respond(
                    HttpStatusCode.Created,
                    CreateUserResponse(
                        id = userInfo.id,
                        username = userInfo.username,
                        password = password,
                        message = "User created. Save the password - it cannot be retrieved later.",
                    ),
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error creating user"))
            }
        }

        delete("/{id}") {
            val session = call.sessions.get<UserSession>()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Not authenticated"))

            val userId = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))

            try {
                val deleted = authService.deleteUser(userId, session.userId)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("User not found"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error deleting user"))
            }
        }

        post("/{id}/reset-password") {
            val session = call.sessions.get<UserSession>()
                ?: return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Not authenticated"))

            val userId = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing id"))

            val newPassword = authService.resetPassword(userId)
            call.respond(
                ResetPasswordResponse(
                    password = newPassword,
                    message = "Password reset. Save the new password - it cannot be retrieved later.",
                ),
            )
        }
    }
}

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val username: String?,
    val message: String?,
)

@Serializable
data class ChangePasswordRequest(
    val newPassword: String,
)

@Serializable
data class CreateUserRequest(
    val username: String,
)

@Serializable
data class UserResponse(
    val id: String,
    val username: String,
    val createdAt: String,
    val lastLoginAt: String?,
)

@Serializable
data class CreateUserResponse(
    val id: String,
    val username: String,
    val password: String,
    val message: String,
)

@Serializable
data class ResetPasswordResponse(
    val password: String,
    val message: String,
)
