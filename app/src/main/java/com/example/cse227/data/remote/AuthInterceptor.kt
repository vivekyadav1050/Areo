package com.example.cse227.data.remote

import com.example.cse227.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip adding Authorization header for the login endpoint
        if (path.contains("/auth/login")) {
            return chain.proceed(originalRequest)
        }

        val token = sessionManager.getAuthToken()
        return if (!token.isNullOrEmpty()) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            val requestWithHeaders = originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build()
            chain.proceed(requestWithHeaders)
        }
    }
}