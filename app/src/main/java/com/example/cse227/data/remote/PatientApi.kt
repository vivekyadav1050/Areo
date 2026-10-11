package com.example.cse227.data.remote

import com.example.cse227.data.model.CreatePatientRequest
import com.example.cse227.data.model.CreatePatientResponse
import com.example.cse227.data.model.PatientStatusRequest
import com.example.cse227.data.model.PatientStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface PatientApi {

    @POST("tablets/{tabletId}/patient")
    suspend fun assignPatient(
        @Path("tabletId") tabletId: String,
        @Body request: CreatePatientRequest
    ): Response<CreatePatientResponse>

    @PATCH("patients/{patientId}/status")
    suspend fun updatePatientStatus(
        @Path("patientId") patientId: String,
        @Body request: PatientStatusRequest
    ): Response<PatientStatusResponse>
}