package com.example.cse227.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReadingDao(private val dbHelper: AppDatabase) {

    private val _pendingCountFlow = MutableStateFlow(queryPendingCount())
    private val _latestReadingFlow = MutableStateFlow<ReadingEntity?>(queryLatestReading())

    private fun refreshFlows() {
        _pendingCountFlow.value = queryPendingCount()
        _latestReadingFlow.value = queryLatestReading()
    }

    private fun queryPendingCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM oximeter_readings WHERE syncStatus = 'PENDING'", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun queryLatestReading(): ReadingEntity? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM oximeter_readings ORDER BY createdAt DESC LIMIT 1", null)
        return cursor.use {
            if (it.moveToFirst()) cursorToEntity(it) else null
        }
    }

    private fun cursorToEntity(cursor: Cursor): ReadingEntity {
        return ReadingEntity(
            recordId = cursor.getString(cursor.getColumnIndexOrThrow("recordId")),
            tabletId = cursor.getString(cursor.getColumnIndexOrThrow("tabletId")),
            patientAssignmentId = cursor.getString(cursor.getColumnIndexOrThrow("patientAssignmentId")),
            deviceId = cursor.getString(cursor.getColumnIndexOrThrow("deviceId")),
            timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp")),
            spo2 = cursor.getInt(cursor.getColumnIndexOrThrow("spo2")),
            pulseRate = cursor.getInt(cursor.getColumnIndexOrThrow("pulseRate")),
            perfusionIndex = if (cursor.isNull(cursor.getColumnIndexOrThrow("perfusionIndex"))) null else cursor.getFloat(cursor.getColumnIndexOrThrow("perfusionIndex")),
            batteryLevel = if (cursor.isNull(cursor.getColumnIndexOrThrow("batteryLevel"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("batteryLevel")),
            probeStatus = cursor.getString(cursor.getColumnIndexOrThrow("probeStatus")),
            syncStatus = cursor.getString(cursor.getColumnIndexOrThrow("syncStatus")),
            retryCount = cursor.getInt(cursor.getColumnIndexOrThrow("retryCount")),
            lastError = cursor.getString(cursor.getColumnIndexOrThrow("lastError")),
            createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt"))
        )
    }

    private fun entityToContentValues(entity: ReadingEntity): ContentValues {
        return ContentValues().apply {
            put("recordId", entity.recordId)
            put("tabletId", entity.tabletId)
            put("patientAssignmentId", entity.patientAssignmentId)
            put("deviceId", entity.deviceId)
            put("timestamp", entity.timestamp)
            put("spo2", entity.spo2)
            put("pulseRate", entity.pulseRate)
            put("perfusionIndex", entity.perfusionIndex)
            put("batteryLevel", entity.batteryLevel)
            put("probeStatus", entity.probeStatus)
            put("syncStatus", entity.syncStatus)
            put("retryCount", entity.retryCount)
            put("lastError", entity.lastError)
            put("createdAt", entity.createdAt)
        }
    }

    suspend fun insertReading(reading: ReadingEntity) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict("oximeter_readings", null, entityToContentValues(reading), SQLiteDatabase.CONFLICT_REPLACE)
        refreshFlows()
    }

    suspend fun insertReadings(readings: List<ReadingEntity>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            for (reading in readings) {
                db.insertWithOnConflict("oximeter_readings", null, entityToContentValues(reading), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshFlows()
    }

    suspend fun getPendingReadings(limit: Int = 100): List<ReadingEntity> {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM oximeter_readings WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC LIMIT ?", arrayOf(limit.toString()))
        val list = mutableListOf<ReadingEntity>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(cursorToEntity(it))
            }
        }
        return list
    }

    fun getPendingCountFlow(): Flow<Int> = _pendingCountFlow.asStateFlow()

    suspend fun getPendingCount(): Int = queryPendingCount()

    suspend fun getSyncedCount(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM oximeter_readings WHERE syncStatus = 'SYNCED'", null)
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    suspend fun markAsSynced(recordIds: List<String>) {
        if (recordIds.isEmpty()) return
        val db = dbHelper.writableDatabase
        val placeholders = recordIds.joinToString(",") { "?" }
        db.execSQL("UPDATE oximeter_readings SET syncStatus = 'SYNCED', lastError = NULL WHERE recordId IN ($placeholders)", recordIds.toTypedArray())
        refreshFlows()
    }

    suspend fun markAsFailed(recordIds: List<String>, error: String?) {
        if (recordIds.isEmpty()) return
        val db = dbHelper.writableDatabase
        for (id in recordIds) {
            db.execSQL("UPDATE oximeter_readings SET syncStatus = 'FAILED', retryCount = retryCount + 1, lastError = ? WHERE recordId = ?", arrayOf(error, id))
        }
        refreshFlows()
    }

        suspend fun deleteAllReadings() {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM oximeter_readings")
        refreshFlows()
    }

    fun getLatestReadingFlow(): Flow<ReadingEntity?> = _latestReadingFlow.asStateFlow()

    suspend fun deleteSyncedBefore(thresholdTimestamp: Long) {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM oximeter_readings WHERE syncStatus = 'SYNCED' AND createdAt < ?", arrayOf(thresholdTimestamp.toString()))
        refreshFlows()
    }

    suspend fun isolateOrClearOldPatientPendingReadings(activePatientId: String) {
        val db = dbHelper.writableDatabase
        db.execSQL("DELETE FROM oximeter_readings WHERE patientAssignmentId != ? AND syncStatus = 'PENDING'", arrayOf(activePatientId))
        refreshFlows()
    }
}
