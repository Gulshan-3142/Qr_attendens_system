package com.college.attendance.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var editUrl by remember(state.backendUrl) { mutableStateOf(state.backendUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = "Student Profile & Identity",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Slate100
        )

        // Digital University ID Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Indigo600, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, null, tint = Slate100, modifier = Modifier.size(32.dp))
                        }

                        Column {
                            Text(
                                text = state.student?.name ?: "Aarav Sharma",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate100
                            )
                            Text(
                                text = state.student?.department ?: "Computer Science & Engineering",
                                fontSize = 12.sp,
                                color = Slate400
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Emerald500.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                    }
                }

                Divider(color = Slate800)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow(label = "University Roll No", value = state.student?.rollNumber ?: "22CS101")
                    InfoRow(label = "College ID", value = state.student?.collegeId ?: "C2022101")
                    InfoRow(label = "Semester", value = "Semester ${state.student?.semester ?: 6}")
                    InfoRow(label = "Email", value = state.student?.email ?: "aarav.sharma@college.edu")
                    InfoRow(label = "Mobile", value = state.student?.phone ?: "+91 98765 43210")
                }
            }
        }

        // Server API Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dns, null, tint = Indigo400)
                    Text("Backend Server Endpoint", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                }

                Text(
                    text = "Use 10.138.216.222for Android Studio Emulator or your local Wi-Fi IP for real devices.",
                    fontSize = 12.sp,
                    color = Slate400
                )

                OutlinedTextField(
                    value = editUrl,
                    onValueChange = { editUrl = it },
                    label = { Text("API URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo500,
                        unfocusedBorderColor = Slate800,
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100
                    )
                )

                Button(
                    onClick = { viewModel.updateBackendUrl(editUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                ) {
                    Text("Save Endpoint", color = Indigo400, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Sign Out Button
        Button(
            onClick = { viewModel.logout(onLoggedOut) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Rose600.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.Logout, null, tint = Rose400)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out of Device", fontWeight = FontWeight.Bold, color = Rose400)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Slate400)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate100)
    }
}
