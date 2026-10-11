package com.example.cse227.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cse227.bluetooth.Ap20BleManager
import com.example.cse227.bluetooth.Ap20DeviceState
import com.example.cse227.bluetooth.Spo2Measurement
import com.example.cse227.data.local.ReadingEntity
import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.ActivePatient
import com.example.cse227.data.repository.MonitoringRepository
import com.example.cse227.data.repository.TabletInfoResult
import com.example.cse227.data.repository.TabletRepository
import com.example.cse227.data.sync.SyncCoordinator
import com.example.cse227.data.sync.SyncStatusState
import com.example.cse227.service.MonitoringForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tabletId: String? = null,
    val activePatient: ActivePatient? = null,
    val latestMeasurement: Spo2Measurement? = null,
    val pendingUploadCount: Int = 0,
    val syncedCount: Int = 0,
    val isMonitoringActive: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val tabletRepository: TabletRepository,
    private val monitoringRepository: MonitoringRepository,
    private val sessionManager: SessionManager,
    private val bleManager: Ap20BleManager,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    val deviceStates: StateFlow<Map<String, Ap20DeviceState>> = bleManager.deviceStates

    val latestReadingFromDb: StateFlow<ReadingEntity?> = monitoringRepository.getLatestReadingFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val pendingCountFlow: StateFlow<Int> = monitoringRepository.getPendingCountFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val syncState: StateFlow<SyncStatusState> = syncCoordinator.syncState
    val lastSyncTime: StateFlow<Long> = syncCoordinator.lastSyncTime
    val lastSyncError: StateFlow<String?> = syncCoordinator.lastSyncError

    private val _tabletState = MutableStateFlow<ActivePatient?>(sessionManager.getCachedActivePatient())
    val activePatient: StateFlow<ActivePatient?> = _tabletState

    private val _tabletId = MutableStateFlow<String?>(sessionManager.getTabletId())
    val tabletId: StateFlow<String?> = _tabletId

    val activeDeviceState: StateFlow<Ap20DeviceState?> = bleManager.deviceStates
        .map { map -> map.values.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        loadTabletAndPatientInfo()
    }

    fun loadTabletAndPatientInfo() {
        val cachedPatient = sessionManager.getCachedActivePatient()
        if (cachedPatient != null) {
            _tabletState.value = cachedPatient
        }
        val cachedTabletId = sessionManager.getTabletId()
        if (!cachedTabletId.isNullOrEmpty()) {
            _tabletId.value = cachedTabletId
        }

        viewModelScope.launch {
            when (val result = tabletRepository.registerTabletIfNeeded()) {
                is TabletInfoResult.Success -> {
                    _tabletId.value = result.tabletId
                    if (result.activePatient != null) {
                        _tabletState.value = result.activePatient
                    }
                }
                is TabletInfoResult.Error -> {
                    when (val fetchResult = tabletRepository.fetchMyTabletInfo()) {
                        is TabletInfoResult.Success -> {
                            _tabletId.value = fetchResult.tabletId
                            if (fetchResult.activePatient != null) {
                                _tabletState.value = fetchResult.activePatient
                            }
                        }
                        is TabletInfoResult.Error -> {}
                    }
                }
            }
        }
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            syncCoordinator.performSyncNow()
        }
    }

    fun startMonitoringService(context: Context) {
        MonitoringForegroundService.startMonitoringService(context)
    }

    fun stopMonitoringService(context: Context) {
        MonitoringForegroundService.stopMonitoringService(context)
    }

    class Factory(
        private val tabletRepository: TabletRepository,
        private val monitoringRepository: MonitoringRepository,
        private val sessionManager: SessionManager,
        private val bleManager: Ap20BleManager,
        private val syncCoordinator: SyncCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(tabletRepository, monitoringRepository, sessionManager, bleManager, syncCoordinator) as T
        }
    }
}
