package com.college.attendance

import android.app.Application
import com.college.attendance.data.local.AppDatabase
import com.college.attendance.data.local.SessionManager
import com.college.attendance.util.LocationHelper

class AttendanceApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var locationHelper: LocationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        sessionManager = SessionManager(this)
        locationHelper = LocationHelper(this)
    }
}
