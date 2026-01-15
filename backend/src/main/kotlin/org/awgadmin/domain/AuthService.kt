package org.awgadmin.domain

import at.favre.lib.crypto.bcrypt.BCrypt
import org.awgadmin.data.UserRepository
import java.security.SecureRandom

/**
 * Service for authentication and user management.
 *
 * Security features:
 * - BCrypt password hashing with cost factor 12
 * - Rate limiting for brute-force protection
 * - Secure random password generation
 */
class AuthService(private val userRepository: UserRepository) {

    companion object {
        private const val BCRYPT_COST = 12
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_WINDOW_MINUTES = 15
        private const val DEFAULT_USERNAME = "admin"
        private const val PASSWORD_LENGTH = 24
        private val PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%&*"
    }

    private val secureRandom = SecureRandom()

    /**
     * Initializes the admin user if none exists.
     * Returns the generated password if a new user was created.
     */
    suspend fun initializeAdminUser(): String? {
        if (userRepository.count() > 0) {
            return null
        }

        val password = generateSecurePassword()
        val passwordHash = hashPassword(password)

        userRepository.create(DEFAULT_USERNAME, passwordHash)

        return password
    }

    /**
     * Attempts to authenticate a user.
     * Returns the user if successful, null otherwise.
     *
     * @param ipAddress The client IP for rate limiting
     * @param username The username
     * @param password The plain text password
     * @return User if authentication successful, null otherwise
     * @throws RateLimitException if too many failed attempts
     */
    suspend fun authenticate(
        ipAddress: String,
        username: String,
        password: String,
    ): User? {
        // Check rate limit
        val failedAttempts = userRepository.countRecentFailedAttempts(
            ipAddress = ipAddress,
            windowMinutes = LOCKOUT_WINDOW_MINUTES,
        )

        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            throw RateLimitException(
                "Too many failed login attempts. Please try again in $LOCKOUT_WINDOW_MINUTES minutes."
            )
        }

        val user = userRepository.findByUsername(username)

        if (user == null || !verifyPassword(password, user.passwordHash)) {
            // Record failed attempt
            userRepository.recordLoginAttempt(ipAddress, username, successful = false)
            return null
        }

        // Record successful attempt and update last login
        userRepository.recordLoginAttempt(ipAddress, username, successful = true)
        userRepository.updateLastLogin(user.id)

        return user
    }

    /**
     * Changes the password for a user.
     */
    suspend fun changePassword(userId: String, newPassword: String): Boolean {
        validatePasswordStrength(newPassword)
        val passwordHash = hashPassword(newPassword)
        return userRepository.updatePassword(userId, passwordHash)
    }

    /**
     * Gets all users (without password hashes).
     */
    suspend fun getAllUsers(): List<UserInfo> {
        return userRepository.getAll().map { user ->
            UserInfo(
                id = user.id,
                username = user.username,
                createdAt = user.createdAt.toString(),
                lastLoginAt = user.lastLoginAt?.toString(),
            )
        }
    }

    /**
     * Creates a new user with a generated password.
     */
    suspend fun createUser(username: String): Pair<UserInfo, String> {
        if (userRepository.findByUsername(username) != null) {
            throw IllegalArgumentException("Username already exists")
        }

        val password = generateSecurePassword()
        val passwordHash = hashPassword(password)
        val user = userRepository.create(username, passwordHash)

        return UserInfo(
            id = user.id,
            username = user.username,
            createdAt = user.createdAt.toString(),
            lastLoginAt = null,
        ) to password
    }

    /**
     * Deletes a user.
     */
    suspend fun deleteUser(userId: String, currentUserId: String): Boolean {
        if (userId == currentUserId) {
            throw IllegalArgumentException("Cannot delete your own account")
        }

        // Ensure at least one admin remains
        if (userRepository.count() <= 1) {
            throw IllegalArgumentException("Cannot delete the last admin user")
        }

        return userRepository.delete(userId)
    }

    /**
     * Resets a user's password to a new generated one.
     */
    suspend fun resetPassword(userId: String): String {
        val password = generateSecurePassword()
        val passwordHash = hashPassword(password)
        userRepository.updatePassword(userId, passwordHash)
        return password
    }

    /**
     * Periodically clean up old login attempts.
     */
    suspend fun cleanupOldAttempts() {
        userRepository.cleanupOldAttempts()
    }

    private fun generateSecurePassword(): String {
        return (1..PASSWORD_LENGTH)
            .map { PASSWORD_CHARS[secureRandom.nextInt(PASSWORD_CHARS.length)] }
            .joinToString("")
    }

    private fun hashPassword(password: String): String {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return BCrypt.verifyer().verify(password.toCharArray(), hash).verified
    }

    private fun validatePasswordStrength(password: String) {
        require(password.length >= 12) {
            "Password must be at least 12 characters long"
        }
        require(password.any { it.isUpperCase() }) {
            "Password must contain at least one uppercase letter"
        }
        require(password.any { it.isLowerCase() }) {
            "Password must contain at least one lowercase letter"
        }
        require(password.any { it.isDigit() }) {
            "Password must contain at least one digit"
        }
    }
}

/**
 * Exception thrown when rate limit is exceeded.
 */
class RateLimitException(message: String) : Exception(message)

/**
 * User information without sensitive data.
 */
data class UserInfo(
    val id: String,
    val username: String,
    val createdAt: String,
    val lastLoginAt: String?,
)
