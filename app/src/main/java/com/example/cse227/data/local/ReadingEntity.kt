package com.example.cse227.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "oximeter_readings")
data class ReadingEntity(
    @PrimaryKey val recordId: String,
    val tabletId: String,
    val patientAssignmentId: String?,
    val deviceId: String,
    val timestamp: String,
    val spo2: Int,
    val pulseRate: Int,
    val perfusionIndex: Float? = null,
    val batteryLevel: Int? = null,
    val probeStatus: String? = "unknown",
    val syncStatus: String = SYNC_STATUS_PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SYNC_STATUS_PENDING = "PENDING"
        const val SYNC_STATUS_SYNCED = "SYNCED"
        const val SYNC_STATUS_FAILED = "FAILED"
    }
}