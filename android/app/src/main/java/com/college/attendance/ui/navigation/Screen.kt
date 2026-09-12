package com.college.attendance.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Onboarding : Screen("onboarding", "Onboarding", Icons.Filled.Person, Icons.Outlined.Person)
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Scanner : Screen("scanner", "Scan", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner)
    object Analytics : Screen("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart)
    object Planner : Screen("planner", "Planner", Icons.Filled.Calculate, Icons.Outlined.Calculate)
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle, Icons.Outlined.AccountCircle)

    companion object {
        val bottomNavItems = listOf(Home, Scanner, Analytics, Planner, Profile)
    }
}
