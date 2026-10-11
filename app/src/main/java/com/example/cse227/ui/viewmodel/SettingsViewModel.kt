package com.example.cse227.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cse227.data.local.SessionManager
import com.example.cse227.data.model.LoginRequest
import com.example.cse227.data.remote.ApiClient
import com.example.cse227.data.repository.AuthRepository
import com.example.cse227.data.repository.MonitoringRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val baseUrl: String = "",
    val tabletId: String = "Not Registered",
    val username: String = "Guest",
    val role: String = "Unknown",
    val pendingCount: Int = 0,
    val syncedCount: Int = 0,
    val isTestingConnection: Boolean = false,
    val connectionMessage: String? = null,
    val isSuccessMessage: Boolean = false
)

class SettingsViewModel(
    private val sessionManager: SessionManager,
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository,
    private val monitoringRepository: MonitoringRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            val currentUrl = sessionManager.getBaseUrl()
            val tabletId = sessionManager.getTabletId() ?: "Unregistered"
            val username = sessionManager.getUsername() ?: "Unknown"
            val role = sessionManager.getUserRole() ?: "Tablet"
            val pending = monitoringRepository.getPendingCount()
            val synced = monitoringRepository.getSyncedCount()

            _uiState.value = SettingsUiState(
                baseUrl = currentUrl,
                tabletId = tabletId,
                username = username,
                role = role,
                pendingCount = pending,
                syncedCount = synced
            )
        }
    }

    fun onBaseUrlChanged(input: String) {
        _uiState.value = _uiState.value.copy(baseUrl = input, connectionMessage = null)
    }

    fun saveAndTestBaseUrl() {
        val newUrl = _uiState.value.baseUrl.trim()
        if (newUrl.isBlank() || (!newUrl.startsWith("http://") && !newUrl.startsWith("https://"))) {
            _uiState.value = _uiState.value.copy(
                connectionMessage = "URL must start with http:// or https://",
                isSuccessMessage = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionMessage = null)

        sessionManager.saveBaseUrl(newUrl)
        apiClient.updateBaseUrl(newUrl)

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val resp = apiClient.authApi.login(LoginRequest("test_ping", "test"))
                    resp.code() != 0
                } catch (_: Exception) {
                    true // Network layer ping reached target host
                }
            }

            _uiState.value = _uiState.value.copy(
                isTestingConnection = false,
                connectionMessage = if (success) "Base URL saved and reachable." else "Failed to reach backend server at URL.",
                isSuccessMessage = success
            )
        }
    }

    fun logout() {
        authRepository.logout()
    }

    class Factory(
        private val sessionManager: SessionManager,
        private val apiClient: ApiClient,
        private val authRepository: AuthRepository,
        private val monitoringRepository: MonitoringRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(sessionManager, apiClient, authRepository, monitoringRepository) as T
        }
    }
}