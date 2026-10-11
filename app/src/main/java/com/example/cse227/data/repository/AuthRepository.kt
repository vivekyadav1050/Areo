package com.example.cse227.data.repository

import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.LoginRequest
import com.example.cse227.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val userId: String, val username: String, val role: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager
) {

    suspend fun login(usernameInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        if (usernameInput.isBlank() || passwordInput.isBlank()) {
            return@withContext AuthResult.Error("Username and password are required.")
        }

        try {
            val response = apiClient.authApi.login(
                LoginRequest(username = usernameInput.trim(), password = passwordInput)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && !body.token.isNullOrEmpty() && body.user != null) {
                    val userRole = (body.user.role ?: "tablet").lowercase()
                    if (userRole != "tablet") {
                        return@withContext AuthResult.Error(
                            "Access denied: User account role is '$userRole'. Patient monitoring tablet requires a 'tablet' role."
                        )
                    }

                    val username = body.user.username ?: usernameInput
                    val userId = body.user.effectiveId
                    val role = body.user.role ?: "tablet"

                    sessionManager.saveAuthSession(
                        token = body.token,
                        userId = userId,
                        username = username,
                        role = role
                    )

                    AuthResult.Success(
                        userId = userId,
                        username = username,
                        role = role
                    )
                } else {
                    AuthResult.Error(body?.message ?: "Login failed. Invalid response from server.")
                }
            } else {
                val codeVal = response.code()
                val errorMsg = when (codeVal) {
                    401 -> "Invalid username or password."
                    403 -> "Unauthorized account role."
                    else -> "Server error ($codeVal). Please try again."
                }
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Network error. Please verify backend connection.")
        }
    }

    fun isLoggedIn(): Boolean = sessionManager.isLoggedIn()

    fun isTabletUser(): Boolean = sessionManager.isTabletUser()

    fun getUsername(): String = sessionManager.getUsername() ?: ""

    fun logout() {
        sessionManager.clearSession()
    }
}