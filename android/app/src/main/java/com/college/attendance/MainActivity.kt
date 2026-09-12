package com.college.attendance
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.college.attendance.ui.navigation.AppNavGraph
import com.college.attendance.ui.theme.SmartQRAttendanceTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AttendanceApplication

        setContent {
            SmartQRAttendanceTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    sessionManager = app.sessionManager,
                    database = app.database,
                    locationHelper = app.locationHelper
                )
            }
        }
    }
}
