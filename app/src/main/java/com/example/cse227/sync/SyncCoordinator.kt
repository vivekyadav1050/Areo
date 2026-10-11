package com.example.cse227.data.sync

import com.example.cse227.bluetooth.Ap20BleManager
import com.example.cse227.data.repository.MonitoringRepository
import com.example.cse227.data.repository.SyncResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncStatusState {
    IDLE,
    SYNCING,
    SYNCED_SUCCESS,
    SYNC_ERROR
}

class SyncCoordinator(
    private val monitoringRepository: MonitoringRepository,
    private val bleManager: Ap20BleManager
) {

    private val _syncState = MutableStateFlow(SyncStatusState.IDLE)
    val syncState: StateFlow<SyncStatusState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    private var monitoringJob: Job? = null
    private var syncTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    companion object {
        const val SYNC_INTERVAL_MS = 30_000L // 30 seconds batch interval
    }

    fun startSyncCoordinator() {
        stopSyncCoordinator()

        // 1. Listen for incoming BLE measurements and persist to Room database
        monitoringJob = scope.launch {
            bleManager.measurementsFlow.collect { measurement ->
                monitoringRepository.saveMeasurement(
                    spo2 = measurement.spo2,
                    pulseRate = measurement.pulseRate,
                    perfusionIndex = measurement.perfusionIndex,
                    deviceId = measurement.deviceName
                )
            }
        }

        // 2. Immediate & Periodic 30-second batch synchronization timer
        syncTimerJob = scope.launch {
            performSyncNow()
            while (true) {
                delay(SYNC_INTERVAL_MS)
                performSyncNow()
            }
        }
    }

    suspend fun performSyncNow(): SyncResult {
        _syncState.value = SyncStatusState.SYNCING
        val result = monitoringRepository.uploadPendingBatch()
        when (result) {
            is SyncResult.Success -> {
                _syncState.value = SyncStatusState.SYNCED_SUCCESS
                _lastSyncTime.value = System.currentTimeMillis()
                _lastSyncError.value = null
            }
            is SyncResult.Error -> {
                _syncState.value = SyncStatusState.SYNC_ERROR
                _lastSyncError.value = result.message
            }
            is SyncResult.NoData -> {
                _syncState.value = SyncStatusState.IDLE
            }
        }
        return result
    }

    fun stopSyncCoordinator() {
        monitoringJob?.cancel()
        syncTimerJob?.cancel()
        _syncState.value = SyncStatusState.IDLE
    }
}