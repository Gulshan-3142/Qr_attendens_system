package com.college.attendance.data.api.models

import com.google.gson.annotations.SerializedName

// 1. Leave Calculator
data class LeaveCalculatorRequest(
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("desiredPercentage") val desiredPercentage: Float = 75f
)

data class LeaveCalculatorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("currentPercentage") val currentPercentage: Float,
    @SerializedName("desiredPercentage") val desiredPercentage: Float,
    @SerializedName("canTakeLeave") val canTakeLeave: Boolean,
    @SerializedName("maxMissableClasses") val maxMissableClasses: Int,
    @SerializedName("projectedTotalClasses") val projectedTotalClasses: Int? = null,
    @SerializedName("projectedAttendancePercentage") val projectedAttendancePercentage: Float? = null,
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String? = null
)

// 2. Skip Calculator
data class SkipCalculatorRequest(
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("targetPercentage") val targetPercentage: Float = 75f,
    @SerializedName("skipCount") val skipCount: Int = 1
)

data class SkipScenarioDto(
    @SerializedName("classesSkipped") val classesSkipped: Int,
    @SerializedName("resultingPercentage") val resultingPercentage: Float,
    @SerializedName("isSafe") val isSafe: Boolean
)

data class SkipCalculatorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("currentPercentage") val currentPercentage: Float,
    @SerializedName("targetPercentage") val targetPercentage: Float,
    @SerializedName("maxClassesYouCanSkip") val maxClassesYouCanSkip: Int,
    @SerializedName("simulatedSkipCount") val simulatedSkipCount: Int,
    @SerializedName("expectedAttendanceAfterSkip") val expectedAttendanceAfterSkip: Float,
    @SerializedName("willFallBelowTarget") val willFallBelowTarget: Boolean,
    @SerializedName("scenarios") val scenarios: List<SkipScenarioDto> = emptyList(),
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String? = null
)

// 3. Target Attendance Calculator
data class TargetCalculatorRequest(
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("targetPercentage") val targetPercentage: Float = 75f
)

data class TargetCalculatorResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("currentPercentage") val currentPercentage: Float,
    @SerializedName("targetPercentage") val targetPercentage: Float,
    @SerializedName("targetAlreadyAchieved") val targetAlreadyAchieved: Boolean,
    @SerializedName("requiredConsecutiveClasses") val requiredConsecutiveClasses: Int,
    @SerializedName("projectedPresent") val projectedPresent: Int? = null,
    @SerializedName("projectedTotal") val projectedTotal: Int? = null,
    @SerializedName("achievedPercentage") val achievedPercentage: Float? = null,
    @SerializedName("message") val message: String,
    @SerializedName("error") val error: String? = null
)

// 4. Exact Day Planner
data class PlannerRequest(
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("targetPercentage") val targetPercentage: Float = 75f,
    @SerializedName("avgClassesPerDay") val avgClassesPerDay: Float = 5.0f
)

data class HolidayDto(
    @SerializedName("date") val date: String,
    @SerializedName("holiday_name") val holidayName: String?,
    @SerializedName("day_of_week") val dayOfWeek: String?
)

data class RoadmapDayDto(
    @SerializedName("dayNumber") val dayNumber: Int,
    @SerializedName("classesAttendedOnDay") val classesAttendedOnDay: Int,
    @SerializedName("totalClassesAttendedSoFar") val totalClassesAttendedSoFar: Int,
    @SerializedName("projectedPercentage") val projectedPercentage: Float,
    @SerializedName("status") val status: String
)

data class PlannerResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("currentPercentage") val currentPercentage: Float,
    @SerializedName("targetPercentage") val targetPercentage: Float,
    @SerializedName("requiredConsecutiveClasses") val requiredConsecutiveClasses: Int,
    @SerializedName("avgClassesPerDay") val avgClassesPerDay: Float,
    @SerializedName("estimatedAcademicDaysRequired") val estimatedAcademicDaysRequired: Int,
    @SerializedName("estimatedCalendarDays") val estimatedCalendarDays: Int,
    @SerializedName("upcomingHolidays") val upcomingHolidays: List<HolidayDto> = emptyList(),
    @SerializedName("roadmap") val roadmap: List<RoadmapDayDto> = emptyList(),
    @SerializedName("summary") val summary: Map<String, Any>? = null,
    @SerializedName("error") val error: String? = null
)

// 5. Holiday Impact
data class HolidayImpactRequest(
    @SerializedName("presentClasses") val presentClasses: Int,
    @SerializedName("totalClasses") val totalClasses: Int,
    @SerializedName("targetPercentage") val targetPercentage: Float = 75f,
    @SerializedName("holidaysCount") val holidaysCount: Int = 3,
    @SerializedName("avgClassesPerDay") val avgClassesPerDay: Float = 5.0f
)

data class HolidayImpactResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("currentAttendance") val currentAttendance: Float,
    @SerializedName("targetAttendance") val targetAttendance: Float,
    @SerializedName("holidaysCount") val holidaysCount: Int,
    @SerializedName("classesSuspended") val classesSuspended: Int,
    @SerializedName("academicDaysBeforeTarget") val academicDaysBeforeTarget: Int,
    @SerializedName("calendarDaysWithHolidays") val calendarDaysWithHolidays: Int,
    @SerializedName("holidayImpactSummary") val holidayImpactSummary: String,
    @SerializedName("error") val error: String? = null
)
