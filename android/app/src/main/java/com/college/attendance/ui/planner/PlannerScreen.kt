package com.college.attendance.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.data.api.models.RoadmapDayDto
import com.college.attendance.ui.theme.*

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp)
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Attendance Planner & Calendar",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Exact academic timeline, day-by-day roadmap, and holiday impact preview.",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }
        }

        // Summary Highlight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null, tint = Indigo400)
                        Text(
                            text = "Target Timeline",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Target Goal: ${state.targetPercentage.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                    }

                    Slider(
                        value = state.targetPercentage,
                        onValueChange = { viewModel.updateTarget(it) },
                        valueRange = 75f..95f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate800
                        )
                    )

                    // Estimated metric tiles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Indigo600.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("📅 Estimated Days", fontSize = 11.sp, color = Indigo400)
                                Text(
                                    text = "${state.plannerResult?.estimatedAcademicDaysRequired ?: 0} Days",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Emerald600.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text("📚 Classes Needed", fontSize = 11.sp, color = Emerald400)
                                Text(
                                    text = "${state.plannerResult?.requiredConsecutiveClasses ?: 0} Classes",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                            }
                        }
                    }
                }
            }
        }

        // Mini Calendar Roadmap
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Day-by-Day Recovery Roadmap",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )

                val roadmap = state.plannerResult?.roadmap ?: emptyList()
                if (roadmap.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(roadmap) { day ->
                            RoadmapDayCard(day = day)
                        }
                    }
                } else {
                    Text("Target already achieved or no classes required.", fontSize = 12.sp, color = Slate400)
                }
            }
        }

        // Holiday Impact Preview
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.BeachAccess, null, tint = Amber400)
                        Text(
                            text = "Holiday Impact Preview",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Text(
                        text = "Preview how upcoming institutional holidays postpone attendance recovery targets.",
                        fontSize = 12.sp,
                        color = Slate400
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Upcoming Holidays: ${state.holidaysCount} Days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                    }

                    Slider(
                        value = state.holidaysCount.toFloat(),
                        onValueChange = { viewModel.updateHolidays(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Amber400,
                            activeTrackColor = Amber500,
                            inactiveTrackColor = Slate800
                        )
                    )

                    state.holidayImpactResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Amber500.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${res.classesSuspended} Academic Classes Postponed",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Amber400
                                )
                                Text(
                                    text = res.holidayImpactSummary,
                                    fontSize = 12.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapDayCard(day: RoadmapDayDto) {
    val isDone = day.status == "TARGET_REACHED"
    Box(
        modifier = Modifier
            .width(110.dp)
            .background(if (isDone) Emerald600.copy(alpha = 0.2f) else Slate900, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Day ${day.dayNumber}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )
            Text(
                text = "+${day.classesAttendedOnDay} Classes",
                fontSize = 11.sp,
                color = if (isDone) Emerald400 else Indigo400
            )
            Text(
                text = "${String.format("%.1f", day.projectedPercentage)}%",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDone) Emerald400 else Slate100
            )
        }
    }
}
