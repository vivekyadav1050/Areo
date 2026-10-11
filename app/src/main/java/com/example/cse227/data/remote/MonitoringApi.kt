package com.example.cse227.data.remote

import com.example.cse227.data.model.UploadReadingsRequest
import com.example.cse227.data.model.UploadReadingsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface MonitoringApi {

    @POST("monitoring")
    suspend fun uploadReadings(
        @Body request: UploadReadingsRequest
    ): Response<UploadReadingsResponse>
}