package com.college.attendance.data.local.dao

import androidx.room.*
import com.college.attendance.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // Student Profile
    @Query("SELECT * FROM cached_student WHERE id = :studentId LIMIT 1")
    suspend fun getCachedStudent(studentId: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStudent(student: StudentEntity)

    // Summary
    @Query("SELECT * FROM cached_summary WHERE studentId = :studentId LIMIT 1")
    fun getCachedSummaryFlow(studentId: String): Flow<SummaryEntity?>

    @Query("SELECT * FROM cached_summary WHERE studentId = :studentId LIMIT 1")
    suspend fun getCachedSummary(studentId: String): SummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSummary(summary: SummaryEntity)

    // Subjects
    @Query("SELECT * FROM cached_subjects WHERE studentId = :studentId")
    suspend fun getCachedSubjects(studentId: String): List<SubjectEntity>

    @Query("DELETE FROM cached_subjects WHERE studentId = :studentId")
    suspend fun deleteSubjects(studentId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSubjects(subjects: List<SubjectEntity>)

    // History
    @Query("SELECT * FROM cached_history WHERE studentId = :studentId ORDER BY date DESC, time DESC")
    suspend fun getCachedHistory(studentId: String): List<HistoryRecordEntity>

    @Query("DELETE FROM cached_history WHERE studentId = :studentId")
    suspend fun deleteHistory(studentId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistory(records: List<HistoryRecordEntity>)

    // Clear all
    @Query("DELETE FROM cached_student")
    suspend fun clearAll()
}
