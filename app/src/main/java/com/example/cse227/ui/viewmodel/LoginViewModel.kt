package com.example.cse227.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cse227.data.repository.AuthRepository
import com.example.cse227.data.repository.AuthResult
import com.example.cse227.data.repository.TabletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val isRegistered: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val tabletRepository: TabletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        if (authRepository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
            registerTabletInBackground()
        }
    }

    fun onUsernameChanged(input: String) {
        _uiState.value = _uiState.value.copy(username = input, errorMessage = null)
    }

    fun onPasswordChanged(input: String) {
        _uiState.value = _uiState.value.copy(password = input, errorMessage = null)
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
    }

    fun login() {
        val current = _uiState.value
        if (current.isLoading) return

        if (current.username.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Username and password cannot be empty.")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            when (val result = authRepository.login(current.username, current.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        errorMessage = null
                    )
                    registerTabletInBackground()
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    private fun registerTabletInBackground() {
        viewModelScope.launch {
            tabletRepository.registerTabletIfNeeded()
            _uiState.value = _uiState.value.copy(isRegistered = true)
        }
    }

    fun resetStateOnLogout() {
        _uiState.value = LoginUiState(isLoggedIn = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val tabletRepository: TabletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(authRepository, tabletRepository) as T
        }
    }
}