package com.college.attendance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.college.attendance.ui.theme.*

enum class StepState {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED
}

data class VerificationSteps(
    val qrCode: StepState = StepState.PENDING,
    val studentAuth: StepState = StepState.PENDING,
    val sessionValidation: StepState = StepState.PENDING,
    val locationGeofence: StepState = StepState.PENDING,
    val attendanceSaved: StepState = StepState.PENDING
)

@Composable
fun VerificationStepDialog(
    steps: VerificationSteps,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Verifying Attendance...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Text(
                    text = "Performing multi-factor server validation",
                    fontSize = 12.sp,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StepItemRow(title = "QR Code & Dynamic Signature", state = steps.qrCode)
                    StepItemRow(title = "Student Identity & Enrollment", state = steps.studentAuth)
                    StepItemRow(title = "Classroom Session Active Check", state = steps.sessionValidation)
                    StepItemRow(title = "GPS Geofence & Anti-Spoofing (<=20m)", state = steps.locationGeofence)
                    StepItemRow(title = "Recording Authoritative Attendance", state = steps.attendanceSaved)
                }
            }
        }
    }
}

@Composable
private fun StepItemRow(
    title: String,
    state: StepState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (state) {
            StepState.PENDING -> {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = Slate600,
                    modifier = Modifier.size(20.dp)
                )
            }
            StepState.IN_PROGRESS -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Indigo400,
                    modifier = Modifier.size(20.dp)
                )
            }
            StepState.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Emerald400,
                    modifier = Modifier.size(20.dp)
                )
            }
            StepState.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = Rose400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (state == StepState.IN_PROGRESS) FontWeight.SemiBold else FontWeight.Normal,
            color = when (state) {
                StepState.PENDING -> Slate400
                StepState.IN_PROGRESS -> Indigo300
                StepState.SUCCESS -> Slate100
                StepState.FAILED -> Rose400
            }
        )
    }
}

val Indigo300 = Color(0xFFA5B4FC)
