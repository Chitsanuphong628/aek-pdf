package com.aekpdf.app.features.camera

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aekpdf.app.R
import com.aekpdf.app.core.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RapidCameraScreen(
    onDismiss: () -> Unit,
    onComplete: (List<Uri>) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    if (!hasCameraPermission) {
        CameraPermissionFallback(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onDismiss = onDismiss
        )
        return
    }

    val capturedUris = remember { mutableStateListOf<Uri>() }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var showFlashOverlay by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("RapidCameraScreen", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    val takePicture: () -> Unit = {
        if (!isCapturing) {
            isCapturing = true
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            // Visual shutter flash animation
            showFlashOverlay = true
            coroutineScope.launch {
                delay(100)
                showFlashOverlay = false
            }

            val scansDir = File(context.cacheDir, "rapid_scans").apply {
                if (!exists()) mkdirs()
            }
            val photoFile = File(scansDir, "scan_${System.currentTimeMillis()}_${capturedUris.size + 1}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        val savedUri = Uri.fromFile(photoFile)
                        capturedUris.add(savedUri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        Log.e("RapidCameraScreen", "Photo capture failed: ${exception.message}", exception)
                    }
                }
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Viewfinder
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Document framing guide overlay
        DocumentFrameOverlay(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 96.dp)
        )

        // Visual flash feedback on capture
        AnimatedVisibility(
            visible = showFlashOverlay,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.55f))
            )
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dismiss / Back button
            IconButton(
                onClick = {
                    if (capturedUris.isNotEmpty()) {
                        showCancelDialog = true
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "ปิด",
                    tint = Color.White
                )
            }

            // Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (capturedUris.isEmpty()) {
                        "โหมดถ่ายรัวต่อเนื่อง"
                    } else {
                        "สแกนแล้ว ${capturedUris.size} หน้า"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Flash Mode Toggle Button
            IconButton(
                onClick = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                    imageCapture.flashMode = flashMode
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (flashMode != ImageCapture.FLASH_MODE_OFF) AppleOrange.copy(alpha = 0.85f)
                        else Color.Black.copy(alpha = 0.45f)
                    )
            ) {
                Icon(
                    imageVector = when (flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                        else -> Icons.Default.FlashOff
                    },
                    contentDescription = "สลับโหมดแฟลช",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Last Captured Thumbnail + Badge (symmetric with Right 100.dp)
                Box(
                    modifier = Modifier.width(100.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (capturedUris.isNotEmpty()) {
                        val lastUri = capturedUris.last()
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(lastUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "หน้าล่าสุด",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Badge count
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .clip(CircleShape)
                                .background(AppleRed)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${capturedUris.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // Empty indicator
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 62.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        )
                    }
                }

                // Center: Apple Shutter Button
                ShutterButton(
                    onClick = takePicture,
                    isEnabled = !isCapturing
                )

                // Right: Done (เสร็จสิ้น) Pill Button
                Box(
                    modifier = Modifier.width(100.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (capturedUris.isNotEmpty()) {
                        Button(
                            onClick = { onComplete(capturedUris.toList()) },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "เสร็จสิ้น",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancel Confirmation Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "ยกเลิกการสแกน?",
                    fontWeight = FontWeight.Bold,
                    color = AppleTextPrimary
                )
            },
            text = {
                Text(
                    text = "มีภาพที่ถ่ายไว้แล้ว ${capturedUris.size} หน้า ต้องการยกเลิกและทิ้งภาพหรือไม่",
                    fontSize = 14.sp,
                    color = AppleTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        onDismiss()
                    }
                ) {
                    Text(
                        text = "ทิ้งภาพและออก",
                        color = AppleRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(
                        text = "ถ่ายต่อ",
                        color = AppleBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            containerColor = AppleCardSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun ShutterButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        label = "shutter_scale"
    )

    Box(
        modifier = modifier
            .size(78.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isEnabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(4.dp, Color.White, CircleShape)
        )

        // Inner solid shutter core
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(if (isPressed) AppleTextSecondary else Color.White)
        )
    }
}

@Composable
private fun DocumentFrameOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val cornerLength = 36.dp.toPx()
        val strokeWidth = 2.5.dp.toPx()
        val frameColor = Color.White.copy(alpha = 0.55f)

        val w = size.width
        val h = size.height

        // Top-Left corner
        val tl = Path().apply {
            moveTo(0f, cornerLength)
            lineTo(0f, 0f)
            lineTo(cornerLength, 0f)
        }
        drawPath(tl, frameColor, style = Stroke(strokeWidth))

        // Top-Right corner
        val tr = Path().apply {
            moveTo(w - cornerLength, 0f)
            lineTo(w, 0f)
            lineTo(w, cornerLength)
        }
        drawPath(tr, frameColor, style = Stroke(strokeWidth))

        // Bottom-Left corner
        val bl = Path().apply {
            moveTo(0f, h - cornerLength)
            lineTo(0f, h)
            lineTo(cornerLength, h)
        }
        drawPath(bl, frameColor, style = Stroke(strokeWidth))

        // Bottom-Right corner
        val br = Path().apply {
            moveTo(w - cornerLength, h)
            lineTo(w, h)
            lineTo(w, h - cornerLength)
        }
        drawPath(br, frameColor, style = Stroke(strokeWidth))
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        containerColor = AppleCanvas,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ปิด",
                        tint = AppleTextPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleCard(cornerRadius = 24.dp, elevation = 2.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.appleSquircleGradientBadge(
                            colors = listOf(AppleBlue, AppleIndigo),
                            size = 64.dp,
                            cornerRadius = 18.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "ต้องการสิทธิ์การเข้าถึงกล้อง",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = AppleTextPrimary
                    )

                    Text(
                        text = "กรุณาอนุญาตให้แอปเข้าถึงกล้อง เพื่อเปิดโหมดถ่ายเอกสารหลายหน้าอย่างต่อเนื่อง",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = AppleTextSecondary,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                    ) {
                        Text(
                            text = "อนุญาตการใช้กล้อง",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
