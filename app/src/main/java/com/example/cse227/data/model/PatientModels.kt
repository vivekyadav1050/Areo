package com.example.cse227.data.model

import com.google.gson.annotations.SerializedName

data class CreatePatientRequest(
    @SerializedName("patientName") val patientName: String,
    @SerializedName("age") val age: Int,
    @SerializedName("sex") val sex: String,
    @SerializedName("date") val date: String
)

data class CreatePatientResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("patient") val patient: ActivePatient? = null,
    @SerializedName("patientId") val patientId: String? = null
)

data class PatientStatusRequest(
    @SerializedName("isActive") val isActive: Boolean
)

data class PatientStatusResponse(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null
)