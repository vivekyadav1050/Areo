package com.example.cse227.data.repository

import android.content.Context
import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.ActivePatient
import com.example.cse227.data.model.RegisterTabletRequest
import com.example.cse227.data.remote.ApiClient
import com.example.cse227.util.DeviceKeyGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class TabletInfoResult {
    data class Success(val tabletId: String, val activePatient: ActivePatient?) : TabletInfoResult()
    data class Error(val message: String) : TabletInfoResult()
}

class TabletRepository(
    private val context: Context,
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager
) {

    fun getDeviceKey(): String {
        return DeviceKeyGenerator.getOrCreateDeviceKey(context)
    }

    suspend fun registerTabletIfNeeded(): TabletInfoResult = withContext(Dispatchers.IO) {
        val existingTabletId = sessionManager.getTabletId()
        val deviceKey = getDeviceKey()

        try {
            val response = apiClient.tabletApi.registerTablet(RegisterTabletRequest(deviceKey = deviceKey))
            if (response.isSuccessful) {
                val body = response.body()
                val registeredId = body?.tabletId
                if (body?.success == true && !registeredId.isNullOrEmpty()) {
                    sessionManager.saveTabletId(registeredId)
                    return@withContext fetchMyTabletInfoInternal()
                }
            }
        } catch (e: Exception) {
            // Fall back to stored registration if offline
        }

        if (!existingTabletId.isNullOrEmpty()) {
            fetchMyTabletInfoInternal()
        } else {
            TabletInfoResult.Error("Tablet registration required. Please check network connection.")
        }
    }

    suspend fun fetchMyTabletInfo(): TabletInfoResult = withContext(Dispatchers.IO) {
        fetchMyTabletInfoInternal()
    }

    private suspend fun fetchMyTabletInfoInternal(): TabletInfoResult {
        return try {
            val response = apiClient.tabletApi.getMyTabletInfo()
            if (response.isSuccessful) {
                val body = response.body()
                val tabletId = body?.effectiveTabletId ?: sessionManager.getTabletId()
                if (!tabletId.isNullOrEmpty()) {
                    sessionManager.saveTabletId(tabletId)
                    val serverPatient = body?.effectivePatient
                    val cached = sessionManager.getCachedActivePatient()
                    val effectivePatient = if (serverPatient != null && !serverPatient.displayName.isBlank() && serverPatient.displayName != "Unassigned Patient") {
                        sessionManager.saveActivePatientInfo(
                            patientId = serverPatient.displayId,
                            patientName = serverPatient.displayName,
                            age = serverPatient.age,
                            sex = serverPatient.sex,
                            date = serverPatient.displayDate
                        )
                        serverPatient
                    } else {
                        cached
                    }
                    TabletInfoResult.Success(tabletId = tabletId, activePatient = effectivePatient)
                } else {
                    TabletInfoResult.Error("No tablet registration record found.")
                }
            } else {
                val storedId = sessionManager.getTabletId()
                val cached = sessionManager.getCachedActivePatient()
                if (!storedId.isNullOrEmpty()) {
                    TabletInfoResult.Success(tabletId = storedId, activePatient = cached)
                } else {
                    TabletInfoResult.Error("Failed to fetch tablet information (${response.code()}).")
                }
            }
        } catch (e: Exception) {
            val storedId = sessionManager.getTabletId()
            val cached = sessionManager.getCachedActivePatient()
            if (!storedId.isNullOrEmpty()) {
                TabletInfoResult.Success(tabletId = storedId, activePatient = cached)
            } else {
                TabletInfoResult.Error(e.message ?: "Failed to connect to backend server.")
            }
        }
    }
}