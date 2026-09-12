package com.college.attendance.data.api

import com.college.attendance.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface AttendanceApiService {

    // Auth
    @POST("auth/student/verify")
    suspend fun verifyStudent(
        @Body request: VerifyStudentRequest
    ): Response<VerifyStudentResponse>

    // Student Profile & Attendance
    @GET("student/profile")
    suspend fun getStudentProfile(
        @Query("studentId") studentId: String
    ): Response<StudentProfileResponse>

    @GET("student/attendance/summary")
    suspend fun getAttendanceSummary(
        @Query("studentId") studentId: String
    ): Response<AttendanceSummaryResponse>

    @GET("student/attendance/subjects")
    suspend fun getSubjectWiseAttendance(
        @Query("studentId") studentId: String
    ): Response<SubjectAttendanceResponse>

    @GET("student/attendance/history")
    suspend fun getAttendanceHistory(
        @Query("studentId") studentId: String,
        @Query("subjectId") subjectId: String? = null,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<AttendanceHistoryResponse>

    @GET("student/analytics/trends")
    suspend fun getAttendanceTrends(
        @Query("studentId") studentId: String,
        @Query("period") period: String = "7days"
    ): Response<AttendanceTrendsResponse>

    // QR Scan & Geofence
    @POST("attendance/scan")
    suspend fun scanAttendance(
        @Body request: ScanAttendanceRequest
    ): Response<ScanAttendanceResponse>

    // Calculators
    @POST("student/calculators/leave")
    suspend fun calculateLeave(
        @Body request: LeaveCalculatorRequest
    ): Response<LeaveCalculatorResponse>

    @POST("student/calculators/skip")
    suspend fun calculateSkip(
        @Body request: SkipCalculatorRequest
    ): Response<SkipCalculatorResponse>

    @POST("student/calculators/target")
    suspend fun calculateTarget(
        @Body request: TargetCalculatorRequest
    ): Response<TargetCalculatorResponse>

    @POST("student/calculators/planner")
    suspend fun calculatePlanner(
        @Body request: PlannerRequest
    ): Response<PlannerResponse>

    @POST("student/calculators/holiday-impact")
    suspend fun calculateHolidayImpact(
        @Body request: HolidayImpactRequest
    ): Response<HolidayImpactResponse>
}
