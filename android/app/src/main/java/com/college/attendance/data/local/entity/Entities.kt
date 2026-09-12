package com.college.attendance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_student")
data class StudentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val rollNumber: String,
    val collegeId: String,
    val email: String?,
    val phone: String?,
    val department: String?,
    val semester: Int,
    val avatarUrl: String?
)

@Entity(tableName = "cached_summary")
data class SummaryEntity(
    @PrimaryKey val studentId: String,
    val attendancePercentage: Float,
    val presentClasses: Int,
    val absentClasses: Int,
    val totalClasses: Int,
    val academicDays: Int,
    val averageClassesPerDay: Float,
    val statusLabel: String
)

@Entity(tableName = "cached_subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val studentId: String,
    val subjectCode: String,
    val subjectName: String,
    val teacherName: String,
    val presentClasses: Int,
    val absentClasses: Int,
    val totalClasses: Int,
    val attendancePercentage: Float,
    val trend: String
)

@Entity(tableName = "cached_history")
data class HistoryRecordEntity(
    @PrimaryKey val attendanceId: String,
    val studentId: String,
    val date: String,
    val time: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val roomName: String?,
    val status: String,
    val distanceMeters: Float?,
    val timestamp: String
)
