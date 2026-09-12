package com.college.attendance.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.RetrofitClient
import com.college.attendance.data.api.models.StudentDto
import com.college.attendance.data.local.SessionManager
import com.college.attendance.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val student: StudentDto? = null,
    val backendUrl: String = "https://qr-attendens-system.onrender.com/api/"
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        val student = authRepository.getLoggedInStudent()
        val url = sessionManager.getBackendUrl()
        _uiState.value = ProfileUiState(student = student, backendUrl = url)
    }

    fun updateBackendUrl(url: String) {
        sessionManager.setBackendUrl(url)
        RetrofitClient.setBaseUrl(url)
        _uiState.value = _uiState.value.copy(backendUrl = url)
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
