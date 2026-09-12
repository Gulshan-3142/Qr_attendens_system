package com.college.attendance.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.college.attendance.ui.theme.*
import com.college.attendance.util.DeviceUtils

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("Aarav Sharma") }
    var rollNumber by remember { mutableStateOf("22CS101") }
    var collegeId by remember { mutableStateOf("C2022101") }
    var email by remember { mutableStateOf("aarav.sharma@college.edu") }
    var phone by remember { mutableStateOf("+91 98765 43210") }

    LaunchedEffect(uiState) {
        if (uiState is OnboardingUiState.Success) {
            onVerificationSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Indigo600, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Slate100,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "Student Onboarding",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Slate100
            )

            Text(
                text = "Verify your credentials against university records to enable secure QR & geofenced attendance marking.",
                fontSize = 13.sp,
                color = Slate400,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Indigo400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )

                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("University Roll Number *") },
                        leadingIcon = { Icon(Icons.Default.Badge, null, tint = Indigo400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = collegeId,
                        onValueChange = { collegeId = it },
                        label = { Text("College ID / Registration ID *") },
                        leadingIcon = { Icon(Icons.Default.School, null, tint = Indigo400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Indigo400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Mobile Number (Optional)") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Indigo400) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            // Error Display
            if (uiState is OnboardingUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Rose600.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Error, null, tint = Rose400)
                        Text(
                            text = (uiState as OnboardingUiState.Error).message,
                            fontSize = 12.sp,
                            color = Rose400,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    val deviceId = DeviceUtils.getUniqueDeviceId(context)
                    val deviceModel = DeviceUtils.getDeviceModelName()
                    viewModel.verifyStudent(name, rollNumber, collegeId, email, phone, deviceId, deviceModel)
                },
                enabled = uiState !is OnboardingUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) {
                if (uiState is OnboardingUiState.Loading) {
                    CircularProgressIndicator(color = Slate100, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify & Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Slate100)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Indigo500,
    unfocusedBorderColor = Slate800,
    focusedLabelColor = Indigo400,
    unfocusedLabelColor = Slate400,
    focusedTextColor = Slate100,
    unfocusedTextColor = Slate100,
    cursorColor = Indigo400
)
