package com.example.cse227.data.remote

import com.example.cse227.data.model.LoginRequest
import com.example.cse227.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
}