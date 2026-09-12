package com.college.attendance.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.SubjectAttendanceDto
import com.college.attendance.data.api.models.TrendDayDto
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: String = "7days", // "7days", "30days", "month", "semester"
    val trends: List<TrendDayDto> = emptyList(),
    val subjects: List<SubjectAttendanceDto> = emptyList(),
    val selectedSubject: SubjectAttendanceDto? = null,
    val errorMessage: String? = null
)

class AnalyticsViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics("7days")
    }

    fun loadAnalytics(period: String) {
        val student = authRepository.getLoggedInStudent() ?: return

        _uiState.value = _uiState.value.copy(isLoading = true, selectedPeriod = period)

        viewModelScope.launch {
            val trendsResult = studentRepository.getTrends(student.id, period)
            val subjectsResult = studentRepository.getSubjects(student.id)

            val subs = subjectsResult.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                selectedPeriod = period,
                trends = trendsResult.getOrDefault(emptyList()),
                subjects = subs,
                selectedSubject = _uiState.value.selectedSubject ?: subs.firstOrNull()
            )
        }
    }

    fun selectSubject(subject: SubjectAttendanceDto) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }
}
