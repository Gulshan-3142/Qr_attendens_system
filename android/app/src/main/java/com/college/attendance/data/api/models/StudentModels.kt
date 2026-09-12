package com.college.attendance.data.api.models

import com.google.gson.annotations.SerializedName

data class StudentDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("rollNumber") val rollNumber: String,
    @SerializedName("collegeId") val collegeId: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("department") val department: String? = null,
    @SerializedName("semester") val semester: Int = 6,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("isDeviceBound") val isDeviceBound: Boolean = false,
    @SerializedName("verified") val verified: Boolean = true,
    @SerializedName("enrolledSubjectsCount") val enrolledSubjectsCount: Int = 0
)

data class VerifyStudentRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("rollNumber") val rollNumber: String,
    @SerializedName("collegeId") val collegeId: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null
)

data class VerifyStudentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("student") val student: StudentDto? = null
)

data class StudentProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("student") val student: StudentDto? = null,
    @SerializedName("error") val error: String? = null
)
