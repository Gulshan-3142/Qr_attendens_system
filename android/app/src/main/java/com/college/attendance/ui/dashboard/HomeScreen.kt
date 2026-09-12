package com.college.attendance.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.ui.components.*
import com.college.attendance.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCalculators: () -> Unit,
    onNavigateToPlanner: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
    ) {
        // 1. Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Hello, ${state.student?.name?.split(" ")?.firstOrNull() ?: "Student"} 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Box(
                            modifier = Modifier
                                .background(Emerald500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VERIFIED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                    }
                    Text(
                        text = "${state.student?.rollNumber ?: "22CS101"} • Sem ${state.student?.semester ?: 6}",
                        fontSize = 13.sp,
                        color = Slate400
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Slate800, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Indigo400,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // 2. Primary Attendance Card (with Gauge + Metrics)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Overall Attendance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate400
                            )
                            Text(
                                text = "${state.summary?.presentClasses ?: 33} of ${state.summary?.totalClasses ?: 40} classes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                        }

                        AttendanceGauge(
                            percentage = state.summary?.attendancePercentage ?: 82.5f,
                            size = 110.dp,
                            strokeWidth = 10.dp
                        )
                    }

                    Divider(color = Slate800)

                    // Stats Grid: Present, Absent, Total, Avg Classes/Day
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniStat(
                            title = "Present",
                            value = "${state.summary?.presentClasses ?: 33}",
                            color = Emerald400
                        )
                        MiniStat(
                            title = "Absent",
                            value = "${state.summary?.absentClasses ?: 7}",
                            color = Rose400
                        )
                        MiniStat(
                            title = "Total",
                            value = "${state.summary?.totalClasses ?: 40}",
                            color = Slate100
                        )
                        MiniStat(
                            title = "Avg/Day",
                            value = "${state.summary?.averageClassesPerDay ?: 5.2}",
                            color = Indigo400
                        )
                    }
                }
            }
        }

        // 3. Quick Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Actions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate300
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        title = "Scan QR",
                        icon = Icons.Default.QrCodeScanner,
                        color = Indigo500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToScan
                    )
                    QuickActionCard(
                        title = "History",
                        icon = Icons.Default.History,
                        color = Cyan500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHistory
                    )
                    QuickActionCard(
                        title = "Calculator",
                        icon = Icons.Default.Calculate,
                        color = Emerald500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToCalculators
                    )
                    QuickActionCard(
                        title = "Planner",
                        icon = Icons.Default.CalendarToday,
                        color = Amber500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPlanner
                    )
                }
            }
        }

        // 4. Recent Attendance Trend Chart
        item {
            TrendBarChart(trends = state.recentTrends)
        }

        // 5. Subject-Wise Attendance Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "${state.subjects.size} Courses",
                    fontSize = 12.sp,
                    color = Slate400
                )
            }
        }

        // Subject Items
        items(state.subjects) { subject ->
            SubjectProgressCard(subject = subject)
        }
    }
}

@Composable
private fun MiniStat(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title.uppercase(), fontSize = 10.sp, color = Slate400)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(Slate900, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Slate300
            )
        }
    }
}
