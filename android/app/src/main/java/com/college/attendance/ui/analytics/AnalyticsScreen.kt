package com.college.attendance.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.ui.components.SubjectProgressCard
import com.college.attendance.ui.components.TrendBarChart
import com.college.attendance.ui.theme.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel
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
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Attendance Analytics",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Interactive trends and subject-wise performance breakdown",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }
        }

        // Period Selector Chips (7 Days, 30 Days, Current Month, Semester)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PeriodChip(label = "Last 7 Days", isSelected = state.selectedPeriod == "7days") {
                    viewModel.loadAnalytics("7days")
                }
                PeriodChip(label = "Last 30 Days", isSelected = state.selectedPeriod == "30days") {
                    viewModel.loadAnalytics("30days")
                }
                PeriodChip(label = "Current Month", isSelected = state.selectedPeriod == "month") {
                    viewModel.loadAnalytics("month")
                }
                PeriodChip(label = "Semester", isSelected = state.selectedPeriod == "semester") {
                    viewModel.loadAnalytics("semester")
                }
            }
        }

        // Trend Chart
        item {
            TrendBarChart(trends = state.trends)
        }

        // Subject Breakdown Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subject Performance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Min 75% Required",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber400
                )
            }
        }

        items(state.subjects) { subject ->
            SubjectProgressCard(subject = subject)
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Indigo600 else Slate900,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Slate100 else Slate400
        )
    }
}
