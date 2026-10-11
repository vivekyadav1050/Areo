package com.example.cse227.data.repository

import com.example.cse227.data.local.ReadingDao
import com.example.cse227.data.local.ReadingEntity
import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.ReadingItem
import com.example.cse227.data.model.UploadReadingsRequest
import com.example.cse227.data.remote.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

sealed class SyncResult {
    data class Success(val uploadedCount: Int, val duplicateCount: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NoData : SyncResult()
}

class MonitoringRepository(
    private val readingDao: ReadingDao,
    private val apiClient: ApiClient,
    private val sessionManager: SessionManager
) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun saveMeasurement(
        spo2: Int,
        pulseRate: Int,
        perfusionIndex: Float? = null,
        deviceId: String = "AP-20"
    ): String = withContext(Dispatchers.IO) {
        val recordId = UUID.randomUUID().toString()
        val tabletId = sessionManager.getTabletId() ?: "UNREGISTERED_TABLET"
        val patientId = sessionManager.getActivePatientId()
        val timestampIso = synchronized(isoDateFormat) { isoDateFormat.format(Date()) }

        val entity = ReadingEntity(
            recordId = recordId,
            tabletId = tabletId,
            patientAssignmentId = patientId,
            deviceId = deviceId,
            timestamp = timestampIso,
            spo2 = spo2,
            pulseRate = pulseRate,
            perfusionIndex = perfusionIndex,
            batteryLevel = null,
            probeStatus = "unknown",
            syncStatus = ReadingEntity.SYNC_STATUS_PENDING
        )

        readingDao.insertReading(entity)
        recordId
    }

    suspend fun uploadPendingBatch(): SyncResult = withContext(Dispatchers.IO) {
        val tabletId = sessionManager.getTabletId()
        if (tabletId.isNullOrEmpty() || tabletId == "UNREGISTERED_TABLET") {
            return@withContext SyncResult.Error("Tablet registration required before synchronization.")
        }

        val pending = readingDao.getPendingReadings(limit = 50)
        if (pending.isEmpty()) {
            return@withContext SyncResult.NoData
        }

        val items = pending.map { entity ->
            ReadingItem(
                recordId = entity.recordId,
                timestamp = entity.timestamp,
                spo2 = entity.spo2,
                pulseRate = entity.pulseRate,
                perfusionIndex = entity.perfusionIndex,
                batteryLevel = entity.batteryLevel ?: 100,
                probeStatus = entity.probeStatus ?: "connected"
            )
        }

        val request = UploadReadingsRequest(
            tabletId = tabletId,
            readings = items
        )

        val recordIds = pending.map { it.recordId }

        try {
            val response = apiClient.monitoringApi.uploadReadings(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    readingDao.markAsSynced(recordIds)
                    SyncResult.Success(
                        uploadedCount = body.inserted,
                        duplicateCount = body.duplicates
                    )
                } else {
                    val msg = body?.message ?: "Upload rejected by backend."
                    readingDao.markAsFailed(recordIds, msg)
                    SyncResult.Error(msg)
                }
            } else {
                val errorMsg = "Server returned code ${response.code()}"
                readingDao.markAsFailed(recordIds, errorMsg)
                SyncResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            val networkError = e.message ?: "Network timeout during batch upload."
            readingDao.markAsFailed(recordIds, networkError)
            SyncResult.Error(networkError)
        }
    }

    fun getPendingCountFlow(): Flow<Int> = readingDao.getPendingCountFlow()

    suspend fun getPendingCount(): Int = readingDao.getPendingCount()

    suspend fun getSyncedCount(): Int = readingDao.getSyncedCount()

    fun getLatestReadingFlow(): Flow<ReadingEntity?> = readingDao.getLatestReadingFlow()
}