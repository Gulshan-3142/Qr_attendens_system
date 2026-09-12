package com.college.attendance.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.ui.theme.*

@Composable
fun AttendanceGauge(
    percentage: Float,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    strokeWidth: Dp = 14.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (percentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 1000),
        label = "gauge_animation"
    )

    val gradientColors = if (percentage >= 75f) {
        listOf(Emerald500, Cyan500, Indigo500)
    } else {
        listOf(Rose500, Amber500)
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            // Background track
            drawArc(
                color = Slate800,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Animated progress arc
            drawArc(
                brush = Brush.sweepGradient(gradientColors),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${String.format("%.1f", percentage)}%",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate100
            )
            Text(
                text = if (percentage >= 75f) "ELIGIBLE" else "LOW ATTENDANCE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (percentage >= 75f) Emerald400 else Rose400
            )
        }
    }
}
