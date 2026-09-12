package com.college.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.college.attendance.data.api.models.ScanAttendanceDetail
import com.college.attendance.ui.theme.*

@Composable
fun AttendanceSuccessDialog(
    attendance: ScanAttendanceDetail,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Success Badge Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Emerald500.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Emerald400,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Attendance Marked Successfully",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100,
                    textAlign = TextAlign.Center
                )

                Divider(color = Slate800)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailRow(label = "Subject", value = "${attendance.subjectName} (${attendance.subjectCode})")
                    DetailRow(label = "Classroom", value = attendance.roomName)
                    DetailRow(label = "Date", value = attendance.date)
                    DetailRow(label = "Time", value = attendance.time)
                    DetailRow(
                        label = "GPS Distance",
                        value = "${String.format("%.1f", attendance.distanceMeters)}m (Within ${attendance.allowedRadiusMeters.toInt()}m zone)"
                    )
                    DetailRow(label = "Status", value = "PRESENT", isStatus = true)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, color = Slate100)
                }
            }
        }
    }
}

@Composable
fun AttendanceFailureDialog(
    errorMessage: String,
    distanceMeters: Float? = null,
    allowedRadiusMeters: Float? = null,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Slate900, RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Failure Badge Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Rose500.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Failed",
                        tint = Rose400,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Attendance Not Marked",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = Slate300,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                if (distanceMeters != null && allowedRadiusMeters != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Slate950, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            DetailRow(label = "Your Distance", value = "${String.format("%.1f", distanceMeters)} meters")
                            DetailRow(label = "Required Geofence", value = "Within ${allowedRadiusMeters.toInt()} meters")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Slate300)
                    }

                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Try Again", fontWeight = FontWeight.Bold, color = Slate100)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isStatus: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Slate400)
        if (isStatus) {
            Box(
                modifier = Modifier
                    .background(Emerald500.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald400
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate100
            )
        }
    }
}
