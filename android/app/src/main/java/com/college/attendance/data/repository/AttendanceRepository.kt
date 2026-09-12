package com.college.attendance.data.repository

import com.college.attendance.data.api.RetrofitClient
import com.college.attendance.data.api.models.ScanAttendanceRequest
import com.college.attendance.data.api.models.ScanAttendanceResponse

class AttendanceRepository {

    suspend fun scanAndMarkAttendance(
        studentId: String,
        qrToken: String,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        isMockLocation: Boolean,
        deviceId: String? = null,
        deviceModel: String? = null
    ): Result<ScanAttendanceResponse> {
        return try {
            val response = RetrofitClient.apiService.scanAttendance(
                ScanAttendanceRequest(
                    studentId = studentId,
                    qrToken = qrToken,
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = accuracy,
                    isMockLocation = isMockLocation,
                    deviceId = deviceId,
                    deviceModel = deviceModel
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.body()
                val errorMsg = errorBody?.error ?: "Attendance scan failed"
                Result.failure(
                    ScanException(
                        message = errorMsg,
                        stepFailed = errorBody?.stepFailed,
                        distanceMeters = errorBody?.distanceMeters,
                        allowedRadiusMeters = errorBody?.allowedRadiusMeters,
                        expired = errorBody?.expired ?: false
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ScanException(
    override val message: String,
    val stepFailed: String? = null,
    val distanceMeters: Float? = null,
    val allowedRadiusMeters: Float? = null,
    val expired: Boolean = false
) : Exception(message)
