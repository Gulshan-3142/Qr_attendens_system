package com.college.attendance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.data.api.models.SubjectAttendanceDto
import com.college.attendance.ui.theme.*

@Composable
fun SubjectProgressCard(
    subject: SubjectAttendanceDto,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (subject.attendancePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "subject_progress"
    )

    val progressColor = if (subject.attendancePercentage >= 75f) Emerald500 else Rose500

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Subject Name + Code + Trend Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Text(
                        text = "${subject.subjectCode} • ${subject.teacherName}",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }

                // Percentage Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(progressColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val (trendIcon, trendTint) = when (subject.trend) {
                        "UP" -> Icons.Default.TrendingUp to Emerald400
                        "DOWN" -> Icons.Default.TrendingDown to Rose400
                        else -> Icons.Default.TrendingFlat to Slate400
                    }
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = null,
                        tint = trendTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "${String.format("%.1f", subject.attendancePercentage)}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Slate800)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(progressColor)
                )
            }

            // Stats breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Present: ${subject.presentClasses} / ${subject.totalClasses}",
                    fontSize = 12.sp,
                    color = Slate300
                )
                Text(
                    text = "Absent: ${subject.absentClasses}",
                    fontSize = 12.sp,
                    color = if (subject.absentClasses > 0) Rose400 else Slate400
                )
                Text(
                    text = if (subject.attendancePercentage >= 75f) "Safe" else "Action Needed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (subject.attendancePercentage >= 75f) Emerald400 else Amber400
                )
            }
        }
    }
}
