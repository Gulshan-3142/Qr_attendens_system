package com.college.attendance.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.college.attendance.data.local.AppDatabase
import com.college.attendance.data.local.SessionManager
import com.college.attendance.data.repository.*
import com.college.attendance.ui.analytics.AnalyticsScreen
import com.college.attendance.ui.analytics.AnalyticsViewModel
import com.college.attendance.ui.calculators.CalculatorsScreen
import com.college.attendance.ui.calculators.CalculatorsViewModel
import com.college.attendance.ui.dashboard.DashboardViewModel
import com.college.attendance.ui.dashboard.HomeScreen
import com.college.attendance.ui.history.HistoryScreen
import com.college.attendance.ui.history.HistoryViewModel
import com.college.attendance.ui.onboarding.OnboardingScreen
import com.college.attendance.ui.onboarding.OnboardingViewModel
import com.college.attendance.ui.planner.PlannerScreen
import com.college.attendance.ui.planner.PlannerViewModel
import com.college.attendance.ui.profile.ProfileScreen
import com.college.attendance.ui.profile.ProfileViewModel
import com.college.attendance.ui.scanner.QRScanViewModel
import com.college.attendance.ui.scanner.QRScannerScreen
import com.college.attendance.util.LocationHelper

@Composable
fun AppNavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    database: AppDatabase,
    locationHelper: LocationHelper
) {
    // Shared Repositories
    val authRepository = AuthRepository(sessionManager, database)
    val studentRepository = StudentRepository(database)
    val attendanceRepository = AttendanceRepository()
    val analyticsRepository = AnalyticsRepository()

    // ViewModels
    val onboardingViewModel = OnboardingViewModel(authRepository)
    val dashboardViewModel = DashboardViewModel(authRepository, studentRepository)
    val qrScanViewModel = QRScanViewModel(authRepository, attendanceRepository, locationHelper)
    val analyticsViewModel = AnalyticsViewModel(authRepository, studentRepository)
    val calculatorsViewModel = CalculatorsViewModel(authRepository, studentRepository, analyticsRepository)
    val plannerViewModel = PlannerViewModel(authRepository, studentRepository, analyticsRepository)
    val historyViewModel = HistoryViewModel(authRepository, studentRepository)
    val profileViewModel = ProfileViewModel(authRepository, sessionManager)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute != Screen.Onboarding.route

    val startDestination = if (authRepository.isVerified()) Screen.Home.route else Screen.Onboarding.route

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        viewModel = onboardingViewModel,
                        onVerificationSuccess = {
                            dashboardViewModel.loadDashboardData()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = dashboardViewModel,
                        onNavigateToScan = { navController.navigate(Screen.Scanner.route) },
                        onNavigateToHistory = { navController.navigate(Screen.History.route) },
                        onNavigateToCalculators = { navController.navigate(Screen.Analytics.route) },
                        onNavigateToPlanner = { navController.navigate(Screen.Planner.route) }
                    )
                }

                composable(Screen.Scanner.route) {
                    QRScannerScreen(
                        viewModel = qrScanViewModel,
                        onNavigateBack = {
                            dashboardViewModel.loadDashboardData()
                            navController.popBackStack()
                        }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(viewModel = analyticsViewModel)
                }

                composable(Screen.Planner.route) {
                    PlannerScreen(viewModel = plannerViewModel)
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = profileViewModel,
                        onLoggedOut = {
                            navController.navigate(Screen.Onboarding.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
