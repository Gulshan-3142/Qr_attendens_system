package com.college.attendance.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.AttendanceRecordDto
import com.college.attendance.data.api.models.SubjectAttendanceDto
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    val records: List<AttendanceRecordDto> = emptyList(),
    val filteredRecords: List<AttendanceRecordDto> = emptyList(),
    val subjects: List<SubjectAttendanceDto> = emptyList(),
    val selectedSubjectId: String? = null,
    val selectedStatus: String? = null,
    val searchQuery: String = "",
    val errorMessage: String? = null
)

class HistoryViewModel(
    private val authRepository: AuthRepository,
    private val studentRepository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val student = authRepository.getLoggedInStudent() ?: return
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            val historyResult = studentRepository.getHistory(student.id)
            val subjectsResult = studentRepository.getSubjects(student.id)

            val recs = historyResult.getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                records = recs,
                filteredRecords = recs,
                subjects = subjectsResult.getOrDefault(emptyList())
            )
            applyFilters()
        }
    }

    fun setSubjectFilter(subjectId: String?) {
        _uiState.value = _uiState.value.copy(selectedSubjectId = subjectId)
        applyFilters()
    }

    fun setStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.records.filter { rec ->
            val matchSubject = state.selectedSubjectId == null || rec.subjectId == state.selectedSubjectId
            val matchStatus = state.selectedStatus == null || rec.status.equals(state.selectedStatus, ignoreCase = true)
            val matchSearch = state.searchQuery.isBlank() ||
                    rec.subjectName.contains(state.searchQuery, ignoreCase = true) ||
                    rec.subjectCode.contains(state.searchQuery, ignoreCase = true) ||
                    (rec.roomName?.contains(state.searchQuery, ignoreCase = true) == true)
            matchSubject && matchStatus && matchSearch
        }
        _uiState.value = state.copy(filteredRecords = filtered)
    }
}
