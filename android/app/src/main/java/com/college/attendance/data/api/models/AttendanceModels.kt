package com.college.attendance.data.api.models

import com.google.gson.annotations.SerializedName

data class AttendanceSummaryDto(
    @SerializedName("attendancePercentage") val attendancePercentage: Float,
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("absentClasses") val absentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("academicDays") val academicDays: Int,
    @SerializedName("averageClassesPerDay") val averageClassesPerDay: Float,
    @SerializedName("statusLabel") val statusLabel: String
)

data class AttendanceSummaryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("summary") val summary: AttendanceSummaryDto? = null,
    @SerializedName("error") val error: String? = null
)

data class SubjectAttendanceDto(
    @SerializedName("subjectId") val subjectId: String,
    @SerializedName("subjectCode") val subjectCode: String,
    @SerializedName("subjectName") val subjectName: String,
    @SerializedName("teacherName") val teacherName: String,
    @SerializedName("credits") val credits: Int,
    @SerializedName("semester") val semester: Int,
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("absentClasses") val absentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("attendancePercentage") val attendancePercentage: Float,
    @SerializedName("trend") val trend: String, // "UP", "DOWN", "STABLE"
    @SerializedName("isLowAttendance") val isLowAttendance: Boolean
)

data class SubjectAttendanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("subjects") val subjects: List<SubjectAttendanceDto> = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class AttendanceRecordDto(
    @SerializedName("attendanceId") val attendanceId: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("subjectId") val subjectId: String,
    @SerializedName("subjectCode") val subjectCode: String,
    @SerializedName("subjectName") val subjectName: String,
    @SerializedName("teacherName") val teacherName: String? = null,
    @SerializedName("roomName") val roomName: String? = null,
    @SerializedName("status") val status: String, // "PRESENT", "ABSENT", "LATE"
    @SerializedName("distanceMeters") val distanceMeters: Float? = null,
    @SerializedName("verificationMethod") val verificationMethod: String? = null,
    @SerializedName("timestamp") val timestamp: String
)

data class AttendanceHistoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("limit") val limit: Int = 20,
    @SerializedName("totalRecords") val totalRecords: Int = 0,
    @SerializedName("records") val records: List<AttendanceRecordDto> = emptyList(),
    @SerializedName("error") val error: String? = null
)

data class ScanAttendanceRequest(
    @SerializedName("studentId") val studentId: String,
    @SerializedName("qrToken") val qrToken: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy") val accuracy: Float,
    @SerializedName("isMockLocation") val isMockLocation: Boolean = false,
    @SerializedName("deviceId") val deviceId: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null
)

data class ScanAttendanceDetail(
    @SerializedName("attendanceId") val attendanceId: String,
    @SerializedName("studentId") val studentId: String,
    @SerializedName("studentName") val studentName: String,
    @SerializedName("rollNumber") val rollNumber: String,
    @SerializedName("subjectName") val subjectName: String,
    @SerializedName("subjectCode") val subjectCode: String,
    @SerializedName("roomName") val roomName: String,
    @SerializedName("date") val date: String,
    @SerializedName("time") val time: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("status") val status: String,
    @SerializedName("locationVerified") val locationVerified: Boolean,
    @SerializedName("distanceMeters") val distanceMeters: Float,
    @SerializedName("allowedRadiusMeters") val allowedRadiusMeters: Float
)

data class ScanAttendanceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("stepFailed") val stepFailed: String? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("expired") val expired: Boolean = false,
    @SerializedName("distanceMeters") val distanceMeters: Float? = null,
    @SerializedName("allowedRadiusMeters") val allowedRadiusMeters: Float? = null,
    @SerializedName("classroomName") val classroomName: String? = null,
    @SerializedName("attendance") val attendance: ScanAttendanceDetail? = null
)

data class TrendDayDto(
    @SerializedName("date") val date: String,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("present") val present: Int,
    @SerializedName("absent") val absent: Int,
    @SerializedName("percentage") val percentage: Float
)

data class AttendanceTrendsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("period") val period: String,
    @SerializedName("trends") val trends: List<TrendDayDto> = emptyList(),
    @SerializedName("error") val error: String? = null
)
