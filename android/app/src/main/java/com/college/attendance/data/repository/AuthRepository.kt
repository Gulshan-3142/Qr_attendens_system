package com.college.attendance.data.repository

import com.college.attendance.data.api.RetrofitClient
import com.college.attendance.data.api.models.StudentDto
import com.college.attendance.data.api.models.VerifyStudentRequest
import com.college.attendance.data.local.AppDatabase
import com.college.attendance.data.local.SessionManager
import com.college.attendance.data.local.entity.StudentEntity

class AuthRepository(
    private val sessionManager: SessionManager,
    private val database: AppDatabase
) {

    init {
        val savedToken = sessionManager.getAuthToken()
        RetrofitClient.setAuthToken(savedToken)
        RetrofitClient.setBaseUrl(sessionManager.getBackendUrl())
    }

    suspend fun verifyStudent(
        name: String?,
        rollNumber: String,
        collegeId: String,
        email: String?,
        phone: String?,
        deviceId: String? = null,
        deviceModel: String? = null
    ): Result<StudentDto> {
        return try {
            val response = RetrofitClient.apiService.verifyStudent(
                VerifyStudentRequest(
                    name = name,
                    rollNumber = rollNumber.trim().uppercase(),
                    collegeId = collegeId.trim().uppercase(),
                    email = email,
                    phone = phone,
                    deviceId = deviceId,
                    deviceModel = deviceModel
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val body = response.body()!!
                val token = body.token ?: ""
                val student = body.student!!

                sessionManager.saveStudentSession(token, student)
                RetrofitClient.setAuthToken(token)

                // Cache in Room
                database.attendanceDao().saveStudent(
                    StudentEntity(
                        id = student.id,
                        name = student.name,
                        rollNumber = student.rollNumber,
                        collegeId = student.collegeId,
                        email = student.email,
                        phone = student.phone,
                        department = student.department,
                        semester = student.semester,
                        avatarUrl = student.avatarUrl
                    )
                )

                Result.success(student)
            } else {
                val errorMsg = response.body()?.error ?: response.errorBody()?.string() ?: "Verification failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getLoggedInStudent(): StudentDto? = sessionManager.getStudentProfile()

    fun isVerified(): Boolean = sessionManager.isStudentVerified()

    suspend fun logout() {
        sessionManager.clearSession()
        RetrofitClient.setAuthToken(null)
        database.attendanceDao().clearAll()
    }
}
