package com.college.attendance.data.repository

import com.college.attendance.data.api.RetrofitClient
import com.college.attendance.data.api.models.*

class AnalyticsRepository {

    suspend fun calculateLeave(
        presentClasses: Int,
        totalClasses: Int,
        desiredPercentage: Float
    ): Result<LeaveCalculatorResponse> {
        return try {
            val response = RetrofitClient.apiService.calculateLeave(
                LeaveCalculatorRequest(presentClasses, totalClasses, desiredPercentage)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to calculate leave"))
            }
        } catch (e: Exception) {
            // Local fallback calculation
            val k = desiredPercentage / 100f
            val maxLeave = Math.floor(((presentClasses - k * totalClasses) / k).toDouble()).toInt()
            val currentPct = if (totalClasses > 0) (presentClasses.toFloat() / totalClasses) * 100f else 0f
            Result.success(
                LeaveCalculatorResponse(
                    success = true,
                    currentPercentage = currentPct,
                    desiredPercentage = desiredPercentage,
                    canTakeLeave = maxLeave > 0,
                    maxMissableClasses = Math.max(0, maxLeave),
                    message = if (maxLeave > 0) "You can miss approximately $maxLeave classes while maintaining >= $desiredPercentage% attendance."
                              else "⚠️ No additional leave is available."
                )
            )
        }
    }

    suspend fun calculateSkip(
        presentClasses: Int,
        totalClasses: Int,
        targetPercentage: Float,
        skipCount: Int
    ): Result<SkipCalculatorResponse> {
        return try {
            val response = RetrofitClient.apiService.calculateSkip(
                SkipCalculatorRequest(presentClasses, totalClasses, targetPercentage, skipCount)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to calculate skip classes"))
            }
        } catch (e: Exception) {
            val currentPct = if (totalClasses > 0) (presentClasses.toFloat() / totalClasses) * 100f else 0f
            val newTotal = totalClasses + skipCount
            val newPct = if (newTotal > 0) (presentClasses.toFloat() / newTotal) * 100f else 0f
            val k = targetPercentage / 100f
            val maxSkip = Math.max(0, Math.floor(((presentClasses - k * totalClasses) / k).toDouble()).toInt())

            Result.success(
                SkipCalculatorResponse(
                    success = true,
                    currentPercentage = currentPct,
                    targetPercentage = targetPercentage,
                    maxClassesYouCanSkip = maxSkip,
                    simulatedSkipCount = skipCount,
                    expectedAttendanceAfterSkip = newPct,
                    willFallBelowTarget = newPct < targetPercentage,
                    message = if (newPct < targetPercentage) "⚠️ Skipping $skipCount classes reduces attendance to ${String.format("%.1f", newPct)}%, below target."
                              else "✅ Skipping $skipCount classes keeps attendance at ${String.format("%.1f", newPct)}%."
                )
            )
        }
    }

    suspend fun calculateTarget(
        presentClasses: Int,
        totalClasses: Int,
        targetPercentage: Float
    ): Result<TargetCalculatorResponse> {
        return try {
            val response = RetrofitClient.apiService.calculateTarget(
                TargetCalculatorRequest(presentClasses, totalClasses, targetPercentage)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to calculate target attendance"))
            }
        } catch (e: Exception) {
            val currentPct = if (totalClasses > 0) (presentClasses.toFloat() / totalClasses) * 100f else 0f
            if (currentPct >= targetPercentage) {
                Result.success(
                    TargetCalculatorResponse(
                        success = true,
                        currentPercentage = currentPct,
                        targetPercentage = targetPercentage,
                        targetAlreadyAchieved = true,
                        requiredConsecutiveClasses = 0,
                        message = "Target already achieved."
                    )
                )
            } else {
                val k = targetPercentage / 100f
                val required = Math.ceil(((k * totalClasses - presentClasses) / (1f - k)).toDouble()).toInt()
                Result.success(
                    TargetCalculatorResponse(
                        success = true,
                        currentPercentage = currentPct,
                        targetPercentage = targetPercentage,
                        targetAlreadyAchieved = false,
                        requiredConsecutiveClasses = required,
                        message = "You must attend $required consecutive classes to reach $targetPercentage% attendance."
                    )
                )
            }
        }
    }

    suspend fun calculatePlanner(
        presentClasses: Int,
        totalClasses: Int,
        targetPercentage: Float,
        avgClassesPerDay: Float
    ): Result<PlannerResponse> {
        return try {
            val response = RetrofitClient.apiService.calculatePlanner(
                PlannerRequest(presentClasses, totalClasses, targetPercentage, avgClassesPerDay)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to load planner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateHolidayImpact(
        presentClasses: Int,
        totalClasses: Int,
        targetPercentage: Float,
        holidaysCount: Int,
        avgClassesPerDay: Float
    ): Result<HolidayImpactResponse> {
        return try {
            val response = RetrofitClient.apiService.calculateHolidayImpact(
                HolidayImpactRequest(presentClasses, totalClasses, targetPercentage, holidaysCount, avgClassesPerDay)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to compute holiday impact"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
