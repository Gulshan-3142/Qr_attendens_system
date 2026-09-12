package com.college.attendance.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.college.attendance.ui.components.AttendanceFailureDialog
import com.college.attendance.ui.components.AttendanceSuccessDialog
import com.college.attendance.ui.components.VerificationStepDialog
import com.college.attendance.ui.theme.*
import com.college.attendance.util.BarcodeAnalyzer
import com.college.attendance.util.DeviceUtils
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    viewModel: QRScanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl: androidx.camera.core.CameraControl? by remember { mutableStateOf(null) }

    // Laser Animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission && hasLocationPermission) {
            // Camera Preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val analyzer = BarcodeAnalyzer { qrContent ->
                            val devId = DeviceUtils.getUniqueDeviceId(ctx)
                            val devModel = DeviceUtils.getDeviceModelName()
                            viewModel.processQrCode(qrContent, devId, devModel)
                        }
                        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Holographic Overlay & Laser
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = size.width * 0.72f
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f - 40.dp.toPx()

                // Semi-transparent dark vignette
                drawRect(
                    color = Color.Black.copy(alpha = 0.55f),
                    size = size
                )

                // Clear center cutout
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    blendMode = BlendMode.Clear
                )

                // Rounded Corner Target Frame
                drawRoundRect(
                    color = Indigo400,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Sweeping Laser Beam
                val beamY = top + (boxSize * laserY)
                drawLine(
                    color = Emerald400,
                    start = Offset(left + 10.dp.toPx(), beamY),
                    end = Offset(left + boxSize - 10.dp.toPx(), beamY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Top Header: Geofence Badge & Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Slate900.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Slate100)
                    }

                    // GPS Accuracy Badge
                    Box(
                        modifier = Modifier
                            .background(Slate900.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).background(Emerald400, CircleShape))
                            Text(
                                text = "GPS Geofence Active (<=20m)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                    }

                    // Flashlight Toggle
                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            cameraControl?.enableTorch(isFlashOn)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Slate900.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Amber400 else Slate300
                        )
                    }
                }

                Text(
                    text = "Align Teacher's Attendance QR inside frame",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate100,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .background(Slate950.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        } else {
            // Permission Denied View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOff,
                    contentDescription = null,
                    tint = Rose400,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permissions Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera & High-Accuracy GPS permissions are required to scan dynamic QR codes and verify physical presence within the 20-meter classroom geofence.",
                    fontSize = 13.sp,
                    color = Slate400,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
                ) {
                    Text("Grant Permissions", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Verification Multi-Step Dialog
        if (uiState is ScanUiState.Verifying) {
            VerificationStepDialog(
                steps = (uiState as ScanUiState.Verifying).steps
            )
        }

        // Success Dialog
        if (uiState is ScanUiState.Success) {
            AttendanceSuccessDialog(
                attendance = (uiState as ScanUiState.Success).attendance,
                onDismiss = {
                    viewModel.resetScanner()
                    onNavigateBack()
                }
            )
        }

        // Failure Dialog
        if (uiState is ScanUiState.Failure) {
            val fail = uiState as ScanUiState.Failure
            AttendanceFailureDialog(
                errorMessage = fail.message,
                distanceMeters = fail.distanceMeters,
                allowedRadiusMeters = fail.allowedRadiusMeters,
                onRetry = { viewModel.resetScanner() },
                onDismiss = {
                    viewModel.resetScanner()
                    onNavigateBack()
                }
            )
        }
    }
}
