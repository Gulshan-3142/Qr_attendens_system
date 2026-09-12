package com.college.attendance.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.college.attendance.ui.theme.*

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        modifier = Modifier.height(72.dp),
        containerColor = Slate900,
        contentColor = Slate100,
        tonalElevation = 8.dp
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        tint = if (isSelected) Indigo400 else Slate400
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        fontSize = 11.sp,
                        color = if (isSelected) Indigo400 else Slate400
                    )
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Indigo400,
                    unselectedIconColor = Slate400,
                    indicatorColor = Indigo600.copy(alpha = 0.2f)
                ),
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}
