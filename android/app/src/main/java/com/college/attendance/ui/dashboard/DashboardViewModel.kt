package com.college.attendance.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.AttendanceSummaryDto
import com.college.attendance.data.api.models.StudentDto
import com.college.attendance.data.api.models.SubjectAttendanceDto
import com.college.attendance.data.api.models.TrendDayDto
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val student: StudentDto? = null,
    val summary: AttendanceSummaryDto? = null,
    val subjects: List<SubjectAttendanceDto> = emptyList(),
    val recentTrends: List<TrendDayDto> = emptyList(),
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        val student = authRepository.getLoggedInStudent()
        if (student == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "No active student session found."
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, student = student, errorMessage = null)

        viewModelScope.launch {
            val summaryResult = studentRepository.getSummary(student.id)
            val subjectsResult = studentRepository.getSubjects(student.id)
            val trendsResult = studentRepository.getTrends(student.id, "7days")

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                summary = summaryResult.getOrNull(),
                subjects = subjectsResult.getOrDefault(emptyList()),
                recentTrends = trendsResult.getOrDefault(emptyList()),
                errorMessage = if (summaryResult.isFailure && subjectsResult.isFailure) "Unable to connect to server. Showing cached data." else null
            )
        }
    }
}
