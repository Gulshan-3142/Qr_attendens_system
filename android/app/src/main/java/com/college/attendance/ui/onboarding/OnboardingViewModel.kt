package com.college.attendance.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.StudentDto
import com.college.attendance.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    data class Success(val student: StudentDto) : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun verifyStudent(
        name: String,
        rollNumber: String,
        collegeId: String,
        email: String,
        phone: String,
        deviceId: String? = null,
        deviceModel: String? = null
    ) {
        if (rollNumber.isBlank() || collegeId.isBlank()) {
            _uiState.value = OnboardingUiState.Error("Roll Number and College ID are required.")
            return
        }

        viewModelScope.launch {
            _uiState.value = OnboardingUiState.Loading
            val result = authRepository.verifyStudent(
                name = name.ifBlank { null },
                rollNumber = rollNumber.trim(),
                collegeId = collegeId.trim(),
                email = email.ifBlank { null },
                phone = phone.ifBlank { null },
                deviceId = deviceId,
                deviceModel = deviceModel
            )

            result.onSuccess { student ->
                _uiState.value = OnboardingUiState.Success(student)
            }.onFailure { exception ->
                _uiState.value = OnboardingUiState.Error(
                    exception.message ?: "Verification failed. Please check your credentials."
                )
            }
        }
    }

    fun resetError() {
        _uiState.value = OnboardingUiState.Idle
    }
}
