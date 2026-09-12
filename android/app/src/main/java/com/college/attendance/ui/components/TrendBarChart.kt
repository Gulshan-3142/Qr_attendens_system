package com.college.attendance.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.data.api.models.TrendDayDto
import com.college.attendance.ui.theme.*

@Composable
fun TrendBarChart(
    trends: List<TrendDayDto>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Slate900, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No recent attendance logs", fontSize = 12.sp, color = Slate400)
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Slate900, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Trend",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendItem(label = "Present", color = Emerald500)
                    LegendItem(label = "Absent", color = Rose500)
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val barCount = trends.size
                val spacing = size.width / (barCount * 2f)
                val barWidth = spacing * 0.8f

                trends.forEachIndexed { index, day ->
                    val x = spacing + index * (2f * spacing)
                    val total = day.totalClasses.coerceAtLeast(1).toFloat()
                    val presentRatio = day.present / total
                    val absentRatio = day.absent / total

                    val totalBarHeight = size.height * 0.85f
                    val presentHeight = totalBarHeight * presentRatio
                    val absentHeight = totalBarHeight * absentRatio

                    // Draw Present bar (Green)
                    if (presentHeight > 0) {
                        drawRoundRect(
                            color = Emerald500,
                            topLeft = Offset(x - barWidth / 2, size.height - presentHeight),
                            size = Size(barWidth, presentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }

                    // Draw Absent bar (Red stacked on top if absent exists)
                    if (absentHeight > 0) {
                        drawRoundRect(
                            color = Rose500,
                            topLeft = Offset(x - barWidth / 2, size.height - presentHeight - absentHeight),
                            size = Size(barWidth, absentHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, fontSize = 10.sp, color = Slate400)
    }
}
