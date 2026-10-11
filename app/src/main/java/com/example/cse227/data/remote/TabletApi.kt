package com.example.cse227.data.remote

import com.example.cse227.data.model.RegisterTabletRequest
import com.example.cse227.data.model.RegisterTabletResponse
import com.example.cse227.data.model.TabletInfoResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TabletApi {

    @POST("tablets/register")
    suspend fun registerTablet(
        @Body request: RegisterTabletRequest
    ): Response<RegisterTabletResponse>

    @GET("tablets/me")
    suspend fun getMyTabletInfo(): Response<TabletInfoResponse>
}