package com.college.attendance.data.local

import android.content.Context
import android.content.SharedPreferences
import com.college.attendance.data.api.models.StudentDto
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("qr_attendance_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_AUTH_TOKEN = "key_auth_token"
        private const val KEY_STUDENT_PROFILE = "key_student_profile"
        private const val KEY_IS_VERIFIED = "key_is_verified"
        private const val KEY_BACKEND_URL = "key_backend_url"
    }

    fun saveStudentSession(token: String, student: StudentDto) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_STUDENT_PROFILE, gson.toJson(student))
            .putBoolean(KEY_IS_VERIFIED, true)
            .apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun getStudentProfile(): StudentDto? {
        val json = prefs.getString(KEY_STUDENT_PROFILE, null) ?: return null
        return try {
            gson.fromJson(json, StudentDto::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun isStudentVerified(): Boolean = prefs.getBoolean(KEY_IS_VERIFIED, false)

    fun getBackendUrl(): String = prefs.getString(KEY_BACKEND_URL, "http://192.168.0.104:3000/api/") ?: "http://192.168.0.104:3000/api/"

    fun setBackendUrl(url: String) {
        prefs.edit().putString(KEY_BACKEND_URL, url).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_STUDENT_PROFILE)
            .putBoolean(KEY_IS_VERIFIED, false)
            .apply()
    }
}
