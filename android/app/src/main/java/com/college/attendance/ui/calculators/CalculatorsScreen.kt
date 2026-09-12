package com.college.attendance.ui.calculators

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.ui.theme.*

@Composable
fun CalculatorsScreen(
    viewModel: CalculatorsViewModel
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
                    text = "Attendance Calculators",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Plan leaves, test class skipping scenarios, and compute target goals.",
                    fontSize = 13.sp,
                    color = Slate400
                )
            }
        }

        // 1. Leave-Day Calculator Card
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
                        Icon(Icons.Default.EventBusy, contentDescription = null, tint = Emerald400)
                        Text(
                            text = "1. Leave-Day Calculator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Text(
                        text = "Calculate how many classes you can miss while remaining above your desired target percentage.",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Desired Minimum: ${state.desiredLeavePercentage.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                    }

                    Slider(
                        value = state.desiredLeavePercentage,
                        onValueChange = { viewModel.calculateLeave(it) },
                        valueRange = 60f..90f,
                        steps = 5,
                        colors = SliderDefaults.colors(
                            thumbColor = Emerald400,
                            activeTrackColor = Emerald500,
                            inactiveTrackColor = Slate800
                        )
                    )

                    // Result Box
                    state.leaveResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (res.canTakeLeave) Emerald500.copy(alpha = 0.12f) else Rose500.copy(alpha = 0.12f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (res.canTakeLeave) "Safe Leaves Available: ${res.maxMissableClasses} Classes" else "⚠️ No Leave Available",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (res.canTakeLeave) Emerald400 else Rose400
                                )
                                Text(
                                    text = res.message,
                                    fontSize = 12.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Skip-Class Simulator Card
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
                        Icon(Icons.Default.FastForward, contentDescription = null, tint = Cyan500)
                        Text(
                            text = "2. Skip-Class Simulator",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Text(
                        text = "Simulate skipping N upcoming classes and see the exact expected percentage impact.",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Classes to Skip: ${state.simulatedSkipCount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate100
                        )
                        Text(
                            text = "Max Safe Skip: ${state.skipResult?.maxClassesYouCanSkip ?: 0}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Cyan500
                        )
                    }

                    Slider(
                        value = state.simulatedSkipCount.toFloat(),
                        onValueChange = { viewModel.calculateSkip(state.targetSkipPercentage, it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan500,
                            activeTrackColor = Cyan500,
                            inactiveTrackColor = Slate800
                        )
                    )

                    state.skipResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (!res.willFallBelowTarget) Cyan500.copy(alpha = 0.12f) else Rose500.copy(alpha = 0.12f),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Projected Attendance: ${String.format("%.1f", res.expectedAttendanceAfterSkip)}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!res.willFallBelowTarget) Cyan500 else Rose400
                                )
                                Text(
                                    text = res.message,
                                    fontSize = 12.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Target Attendance Goal Card
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
                        Icon(Icons.Default.AdsClick, contentDescription = null, tint = Indigo400)
                        Text(
                            text = "3. Target Attendance Goal",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }

                    Text(
                        text = "Calculate how many consecutive classes you must attend to achieve your target percentage.",
                        fontSize = 12.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "Desired Goal: ${state.desiredTargetPercentage.toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate100
                    )

                    Slider(
                        value = state.desiredTargetPercentage,
                        onValueChange = { viewModel.calculateTarget(it) },
                        valueRange = 75f..95f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = Indigo400,
                            activeTrackColor = Indigo500,
                            inactiveTrackColor = Slate800
                        )
                    )

                    state.targetResult?.let { res ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Indigo500.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (res.targetAlreadyAchieved) "Goal Already Achieved!"
                                           else "Required: ${res.requiredConsecutiveClasses} Consecutive Classes",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Indigo400
                                )
                                Text(
                                    text = res.message,
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
