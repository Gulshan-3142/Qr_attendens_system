package com.college.attendance.ui.calculators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.*
import com.college.attendance.data.repository.AnalyticsRepository
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalculatorsUiState(
    val presentClasses: Int = 33,
    val totalClasses: Int = 40,
    val currentPercentage: Float = 82.5f,
    // Calculator 1: Leave Day
    val desiredLeavePercentage: Float = 75f,
    val leaveResult: LeaveCalculatorResponse? = null,
    // Calculator 2: Skip Class
    val targetSkipPercentage: Float = 75f,
    val simulatedSkipCount: Int = 2,
    val skipResult: SkipCalculatorResponse? = null,
    // Calculator 3: Target Attendance
    val desiredTargetPercentage: Float = 85f,
    val targetResult: TargetCalculatorResponse? = null
)

class CalculatorsViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorsUiState())
    val uiState: StateFlow<CalculatorsUiState> = _uiState.asStateFlow()

    init {
        loadInitialStats()
    }

    private fun loadInitialStats() {
        val student = authRepository.getLoggedInStudent() ?: return
        viewModelScope.launch {
            studentRepository.getSummary(student.id).onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    presentClasses = summary.presentClasses,
                    totalClasses = summary.totalClasses,
                    currentPercentage = summary.attendancePercentage
                )
                runAllCalculators()
            }
        }
    }

    fun calculateLeave(desiredPct: Float) {
        _uiState.value = _uiState.value.copy(desiredLeavePercentage = desiredPct)
        viewModelScope.launch {
            analyticsRepository.calculateLeave(
                _uiState.value.presentClasses,
                _uiState.value.totalClasses,
                desiredPct
            ).onSuccess {
                _uiState.value = _uiState.value.copy(leaveResult = it)
            }
        }
    }

    fun calculateSkip(targetPct: Float, skipCount: Int) {
        _uiState.value = _uiState.value.copy(targetSkipPercentage = targetPct, simulatedSkipCount = skipCount)
        viewModelScope.launch {
            analyticsRepository.calculateSkip(
                _uiState.value.presentClasses,
                _uiState.value.totalClasses,
                targetPct,
                skipCount
            ).onSuccess {
                _uiState.value = _uiState.value.copy(skipResult = it)
            }
        }
    }

    fun calculateTarget(targetPct: Float) {
        _uiState.value = _uiState.value.copy(desiredTargetPercentage = targetPct)
        viewModelScope.launch {
            analyticsRepository.calculateTarget(
                _uiState.value.presentClasses,
                _uiState.value.totalClasses,
                targetPct
            ).onSuccess {
                _uiState.value = _uiState.value.copy(targetResult = it)
            }
        }
    }

    private fun runAllCalculators() {
        calculateLeave(_uiState.value.desiredLeavePercentage)
        calculateSkip(_uiState.value.targetSkipPercentage, _uiState.value.simulatedSkipCount)
        calculateTarget(_uiState.value.desiredTargetPercentage)
    }
}
