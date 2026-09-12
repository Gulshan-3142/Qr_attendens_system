package com.college.attendance.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.HolidayImpactResponse
import com.college.attendance.data.api.models.PlannerResponse
import com.college.attendance.data.repository.AnalyticsRepository
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlannerUiState(
    val presentClasses: Int = 33,
    val totalClasses: Int = 40,
    val targetPercentage: Float = 85f,
    val avgClassesPerDay: Float = 5.2f,
    val holidaysCount: Int = 3,
    val plannerResult: PlannerResponse? = null,
    val holidayImpactResult: HolidayImpactResponse? = null,
    val isLoading: Boolean = false
)

class PlannerViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlannerUiState())
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val student = authRepository.getLoggedInStudent() ?: return
        viewModelScope.launch {
            studentRepository.getSummary(student.id).onSuccess { summary ->
                _uiState.value = _uiState.value.copy(
                    presentClasses = summary.presentClasses,
                    totalClasses = summary.totalClasses,
                    avgClassesPerDay = summary.averageClassesPerDay
                )
                refreshPlanner()
            }
        }
    }

    fun updateTarget(target: Float) {
        _uiState.value = _uiState.value.copy(targetPercentage = target)
        refreshPlanner()
    }

    fun updateHolidays(holidays: Int) {
        _uiState.value = _uiState.value.copy(holidaysCount = holidays)
        refreshPlanner()
    }

    private fun refreshPlanner() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pRes = analyticsRepository.calculatePlanner(
                _uiState.value.presentClasses,
                _uiState.value.totalClasses,
                _uiState.value.targetPercentage,
                _uiState.value.avgClassesPerDay
            )

            val hRes = analyticsRepository.calculateHolidayImpact(
                _uiState.value.presentClasses,
                _uiState.value.totalClasses,
                _uiState.value.targetPercentage,
                _uiState.value.holidaysCount,
                _uiState.value.avgClassesPerDay
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                plannerResult = pRes.getOrNull(),
                holidayImpactResult = hRes.getOrNull()
            )
        }
    }
}
