package com.example.cse227.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cse227.data.model.ActivePatient
import com.example.cse227.data.repository.PatientOperationResult
import com.example.cse227.data.repository.PatientRepository
import com.example.cse227.data.repository.TabletInfoResult
import com.example.cse227.data.repository.TabletRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PatientFormUiState(
    val activePatient: ActivePatient? = null,
    val patientName: String = "",
    val age: String = "",
    val sex: String = "Male",
    val date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class PatientViewModel(
    private val patientRepository: PatientRepository,
    private val tabletRepository: TabletRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientFormUiState())
    val uiState: StateFlow<PatientFormUiState> = _uiState.asStateFlow()

    init {
        loadActivePatient()
    }

    fun loadActivePatient() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = tabletRepository.fetchMyTabletInfo()) {
                is TabletInfoResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activePatient = result.activePatient
                    )
                }
                is TabletInfoResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun onPatientNameChanged(input: String) {
        _uiState.value = _uiState.value.copy(patientName = input, errorMessage = null)
    }

    fun onAgeChanged(input: String) {
        _uiState.value = _uiState.value.copy(age = input, errorMessage = null)
    }

    fun onSexChanged(input: String) {
        _uiState.value = _uiState.value.copy(sex = input, errorMessage = null)
    }

    fun onDateChanged(input: String) {
        _uiState.value = _uiState.value.copy(date = input, errorMessage = null)
    }

    fun assignPatient() {
        val current = _uiState.value
        if (current.isLoading) return

        val ageInt = current.age.toIntOrNull()
        if (current.patientName.isBlank()) {
            _uiState.value = current.copy(errorMessage = "Patient name cannot be empty.")
            return
        }
        if (ageInt == null || ageInt <= 0 || ageInt > 130) {
            _uiState.value = current.copy(errorMessage = "Please enter a valid age between 1 and 130.")
            return
        }

        _uiState.value = current.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            val result = patientRepository.assignPatient(
                patientName = current.patientName,
                age = ageInt,
                sex = current.sex,
                date = current.date
            )
            when (result) {
                is PatientOperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activePatient = result.patient,
                        successMessage = result.message,
                        errorMessage = null,
                        patientName = "",
                        age = ""
                    )
                }
                is PatientOperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun deactivatePatient(patientId: String) {
        if (patientId.isBlank()) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)

        viewModelScope.launch {
            when (val result = patientRepository.deactivatePatient(patientId)) {
                is PatientOperationResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activePatient = null,
                        successMessage = result.message
                    )
                }
                is PatientOperationResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    class Factory(
        private val patientRepository: PatientRepository,
        private val tabletRepository: TabletRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PatientViewModel(patientRepository, tabletRepository) as T
        }
    }
}