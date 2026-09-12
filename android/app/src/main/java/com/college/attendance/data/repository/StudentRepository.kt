package com.college.attendance.data.repository

import com.college.attendance.data.api.RetrofitClient
import com.college.attendance.data.api.models.*
import com.college.attendance.data.local.AppDatabase
import com.college.attendance.data.local.entity.HistoryRecordEntity
import com.college.attendance.data.local.entity.SubjectEntity
import com.college.attendance.data.local.entity.SummaryEntity

class StudentRepository(
    private val database: AppDatabase
) {

    suspend fun getSummary(studentId: String): Result<AttendanceSummaryDto> {
        return try {
            val response = RetrofitClient.apiService.getAttendanceSummary(studentId)
            if (response.isSuccessful && response.body()?.success == true) {
                val summary = response.body()!!.summary!!
                
                // Cache to Room
                database.attendanceDao().saveSummary(
                    SummaryEntity(
                        studentId = studentId,
                        attendancePercentage = summary.attendancePercentage,
                        presentClasses = summary.presentClasses,
                        absentClasses = summary.absentClasses,
                        totalClasses = summary.totalClasses,
                        academicDays = summary.academicDays,
                        averageClassesPerDay = summary.averageClassesPerDay,
                        statusLabel = summary.statusLabel
                    )
                )
                Result.success(summary)
            } else {
                // Fallback to cached summary
                val cached = database.attendanceDao().getCachedSummary(studentId)
                if (cached != null) {
                    Result.success(
                        AttendanceSummaryDto(
                            attendancePercentage = cached.attendancePercentage,
                            presentClasses = cached.presentClasses,
                            absentClasses = cached.absentClasses,
                            totalClasses = cached.totalClasses,
                            academicDays = cached.academicDays,
                            averageClassesPerDay = cached.averageClassesPerDay,
                            statusLabel = cached.statusLabel
                        )
                    )
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to load summary"))
                }
            }
        } catch (e: Exception) {
            val cached = database.attendanceDao().getCachedSummary(studentId)
            if (cached != null) {
                Result.success(
                    AttendanceSummaryDto(
                        attendancePercentage = cached.attendancePercentage,
                        presentClasses = cached.presentClasses,
                        absentClasses = cached.absentClasses,
                        totalClasses = cached.totalClasses,
                        academicDays = cached.academicDays,
                        averageClassesPerDay = cached.averageClassesPerDay,
                        statusLabel = cached.statusLabel
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getSubjects(studentId: String): Result<List<SubjectAttendanceDto>> {
        return try {
            val response = RetrofitClient.apiService.getSubjectWiseAttendance(studentId)
            if (response.isSuccessful && response.body()?.success == true) {
                val subjects = response.body()!!.subjects
                
                // Cache to Room
                database.attendanceDao().saveSubjects(
                    subjects.map {
                        SubjectEntity(
                            subjectId = it.subjectId,
                            studentId = studentId,
                            subjectCode = it.subjectCode,
                            subjectName = it.subjectName,
                            teacherName = it.teacherName,
                            presentClasses = it.presentClasses,
                            absentClasses = it.absentClasses,
                            totalClasses = it.totalClasses,
                            attendancePercentage = it.attendancePercentage,
                            trend = it.trend
                        )
                    }
                )
                Result.success(subjects)
            } else {
                val cached = database.attendanceDao().getCachedSubjects(studentId)
                if (cached.isNotEmpty()) {
                    Result.success(cached.map {
                        SubjectAttendanceDto(
                            subjectId = it.subjectId,
                            subjectCode = it.subjectCode,
                            subjectName = it.subjectName,
                            teacherName = it.teacherName,
                            credits = 4,
                            semester = 6,
                            presentClasses = it.presentClasses,
                            absentClasses = it.absentClasses,
                            totalClasses = it.totalClasses,
                            attendancePercentage = it.attendancePercentage,
                            trend = it.trend,
                            isLowAttendance = it.attendancePercentage < 75
                        )
                    })
                } else {
                    Result.failure(Exception(response.body()?.error ?: "Failed to load subjects"))
                }
            }
        } catch (e: Exception) {
            val cached = database.attendanceDao().getCachedSubjects(studentId)
            if (cached.isNotEmpty()) {
                Result.success(cached.map {
                    SubjectAttendanceDto(
                        subjectId = it.subjectId,
                        subjectCode = it.subjectCode,
                        subjectName = it.subjectName,
                        teacherName = it.teacherName,
                        credits = 4,
                        semester = 6,
                        presentClasses = it.presentClasses,
                        absentClasses = it.absentClasses,
                        totalClasses = it.totalClasses,
                        attendancePercentage = it.attendancePercentage,
                        trend = it.trend,
                        isLowAttendance = it.attendancePercentage < 75
                    )
                })
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getHistory(
        studentId: String,
        subjectId: String? = null,
        status: String? = null
    ): Result<List<AttendanceRecordDto>> {
        return try {
            val response = RetrofitClient.apiService.getAttendanceHistory(
                studentId = studentId,
                subjectId = subjectId,
                status = status,
                limit = 100
            )
            if (response.isSuccessful && response.body()?.success == true) {
                val records = response.body()!!.records
                database.attendanceDao().saveHistory(
                    records.map {
                        HistoryRecordEntity(
                            attendanceId = it.attendanceId,
                            studentId = studentId,
                            date = it.date,
                            time = it.time,
                            subjectId = it.subjectId,
                            subjectCode = it.subjectCode,
                            subjectName = it.subjectName,
                            roomName = it.roomName,
                            status = it.status,
                            distanceMeters = it.distanceMeters,
                            timestamp = it.timestamp
                        )
                    }
                )
                Result.success(records)
            } else {
                val cached = database.attendanceDao().getCachedHistory(studentId)
                Result.success(cached.map {
                    AttendanceRecordDto(
                        attendanceId = it.attendanceId,
                        date = it.date,
                        time = it.time,
                        subjectId = it.subjectId,
                        subjectCode = it.subjectCode,
                        subjectName = it.subjectName,
                        roomName = it.roomName,
                        status = it.status,
                        distanceMeters = it.distanceMeters,
                        timestamp = it.timestamp
                    )
                })
            }
        } catch (e: Exception) {
            val cached = database.attendanceDao().getCachedHistory(studentId)
            Result.success(cached.map {
                AttendanceRecordDto(
                    attendanceId = it.attendanceId,
                    date = it.date,
                    time = it.time,
                    subjectId = it.subjectId,
                    subjectCode = it.subjectCode,
                    subjectName = it.subjectName,
                    roomName = it.roomName,
                    status = it.status,
                    distanceMeters = it.distanceMeters,
                    timestamp = it.timestamp
                )
            })
        }
    }

    suspend fun getTrends(studentId: String, period: String = "7days"): Result<List<TrendDayDto>> {
        return try {
            val response = RetrofitClient.apiService.getAttendanceTrends(studentId, period)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.trends)
            } else {
                Result.failure(Exception(response.body()?.error ?: "Failed to load trends"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
