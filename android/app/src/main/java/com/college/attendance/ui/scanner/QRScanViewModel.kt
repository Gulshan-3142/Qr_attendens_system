package com.college.attendance.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.college.attendance.data.api.models.ScanAttendanceDetail
import com.college.attendance.data.repository.AttendanceRepository
import com.college.attendance.data.repository.AuthRepository
import com.college.attendance.data.repository.ScanException
import com.college.attendance.ui.components.StepState
import com.college.attendance.ui.components.VerificationSteps
import com.college.attendance.util.DeviceLocationResult
import com.college.attendance.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    data class Verifying(val steps: VerificationSteps) : ScanUiState()
    data class Success(val attendance: ScanAttendanceDetail) : ScanUiState()
    data class Failure(
        val message: String,
        val distanceMeters: Float? = null,
        val allowedRadiusMeters: Float? = null,
        val isExpired: Boolean = false
    ) : ScanUiState()
}

class QRScanViewModel(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val locationHelper: LocationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var isProcessing = false

    fun processQrCode(
        qrToken: String,
        deviceId: String? = null,
        deviceModel: String? = null
    ) {
        if (isProcessing) return
        isProcessing = true

        val student = authRepository.getLoggedInStudent()
        if (student == null) {
            _uiState.value = ScanUiState.Failure("Student authentication required. Please log in again.")
            isProcessing = false
            return
        }

        viewModelScope.launch {
            // Step 1: QR Code Decoded
            var steps = VerificationSteps(qrCode = StepState.IN_PROGRESS)
            _uiState.value = ScanUiState.Verifying(steps)
            delay(300)
            steps = steps.copy(qrCode = StepState.SUCCESS, studentAuth = StepState.IN_PROGRESS)
            _uiState.value = ScanUiState.Verifying(steps)

            // Step 2: Student Auth Verified
            delay(300)
            steps = steps.copy(studentAuth = StepState.SUCCESS, sessionValidation = StepState.IN_PROGRESS)
            _uiState.value = ScanUiState.Verifying(steps)

            // Step 3: Fetch GPS Location & Integrity Check
            steps = steps.copy(sessionValidation = StepState.SUCCESS, locationGeofence = StepState.IN_PROGRESS)
            _uiState.value = ScanUiState.Verifying(steps)

            val locationResult = locationHelper.getCurrentLocation()
            val location: DeviceLocationResult
            if (locationResult.isSuccess) {
                location = locationResult.getOrThrow()
            } else {
                _uiState.value = ScanUiState.Failure(
                    locationResult.exceptionOrNull()?.message ?: "Failed to obtain GPS location. Please turn on Location Services."
                )
                isProcessing = false
                return@launch
            }

            // Step 4: Submit to Backend Verification Pipeline (Including Device Check)
            steps = steps.copy(locationGeofence = StepState.IN_PROGRESS, attendanceSaved = StepState.IN_PROGRESS)
            _uiState.value = ScanUiState.Verifying(steps)

            val apiResult = attendanceRepository.scanAndMarkAttendance(
                studentId = student.id,
                qrToken = qrToken,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                isMockLocation = location.isMockLocation,
                deviceId = deviceId,
                deviceModel = deviceModel
            )

            apiResult.onSuccess { response ->
                steps = steps.copy(locationGeofence = StepState.SUCCESS, attendanceSaved = StepState.SUCCESS)
                _uiState.value = ScanUiState.Verifying(steps)
                delay(400)
                _uiState.value = ScanUiState.Success(response.attendance!!)
            }.onFailure { err ->
                val scanEx = err as? ScanException
                _uiState.value = ScanUiState.Failure(
                    message = scanEx?.message ?: err.message ?: "Attendance scan rejected by backend.",
                    distanceMeters = scanEx?.distanceMeters,
                    allowedRadiusMeters = scanEx?.allowedRadiusMeters,
                    isExpired = scanEx?.expired ?: false
                )
            }

            isProcessing = false
        }
    }

    fun resetScanner() {
        isProcessing = false
        _uiState.value = ScanUiState.Idle
    }
}
