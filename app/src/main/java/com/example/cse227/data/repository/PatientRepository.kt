package com.example.cse227.data.repository

import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.ActivePatient
import com.example.cse227.data.model.CreatePatientRequest
import com.example.cse227.data.model.PatientStatusRequest
import com.example.cse227.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class PatientOperationResult {
    data class Success(val message: String, val patient: ActivePatient? = null) : PatientOperationResult()
    data class Error(val message: String) : PatientOperationResult()
}

class PatientRepository(
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager
) {

    suspend fun assignPatient(
        patientName: String,
        age: Int,
        sex: String,
        date: String
    ): PatientOperationResult = withContext(Dispatchers.IO) {
        val tabletId = sessionManager.getTabletId()
        if (tabletId.isNullOrEmpty()) {
            return@withContext PatientOperationResult.Error("Tablet registration required before patient setup.")
        }

        if (patientName.isBlank()) {
            return@withContext PatientOperationResult.Error("Patient name is required.")
        }
        if (age <= 0 || age > 130) {
            return@withContext PatientOperationResult.Error("Please enter a valid age.")
        }

        try {
            val request = CreatePatientRequest(
                patientName = patientName.trim(),
                age = age,
                sex = sex.lowercase(),
                date = date
            )
            val response = apiClient.patientApi.assignPatient(tabletId = tabletId, request = request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    val newPatient = body.patient ?: ActivePatient(
                        patientId = body.patientId,
                        mongoId = body.patientId,
                        patientName = patientName,
                        age = age,
                        sex = sex,
                        assignmentDate = date,
                        date = date,
                        isActive = true
                    )
                    sessionManager.saveActivePatientInfo(
                        patientId = newPatient.displayId,
                        patientName = newPatient.displayName,
                        age = newPatient.age ?: age,
                        sex = newPatient.sex ?: sex,
                        date = newPatient.displayDate
                    )
                    PatientOperationResult.Success(
                        message = body.message ?: "Patient successfully assigned.",
                        patient = newPatient
                    )
                } else {
                    PatientOperationResult.Error(body?.message ?: "Failed to assign patient.")
                }
            } else {
                PatientOperationResult.Error("Backend error (${response.code()}): ${response.message()}")
            }
        } catch (e: Exception) {
            PatientOperationResult.Error(e.message ?: "Network error assigning patient.")
        }
    }

    suspend fun deactivatePatient(patientId: String): PatientOperationResult = withContext(Dispatchers.IO) {
        if (patientId.isBlank()) {
            return@withContext PatientOperationResult.Error("Invalid patient ID.")
        }

        try {
            val response = apiClient.patientApi.updatePatientStatus(
                patientId = patientId,
                request = PatientStatusRequest(isActive = false)
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    sessionManager.saveActivePatientInfo(null, null)
                    PatientOperationResult.Success("Patient deactivated successfully.")
                } else {
                    PatientOperationResult.Error(body?.message ?: "Failed to deactivate patient.")
                }
            } else {
                PatientOperationResult.Error("Deactivation failed (${response.code()}).")
            }
        } catch (e: Exception) {
            PatientOperationResult.Error(e.message ?: "Network error during patient deactivation.")
        }
    }
}