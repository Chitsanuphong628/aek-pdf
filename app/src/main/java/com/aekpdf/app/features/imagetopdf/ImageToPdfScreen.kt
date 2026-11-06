package com.aekpdf.app.features.imagetopdf

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aekpdf.app.R
import com.aekpdf.app.core.model.ExportQuality
import com.aekpdf.app.core.model.PageFilter
import com.aekpdf.app.core.model.PageFormat
import com.aekpdf.app.core.scanner.DocumentScannerHelper
import com.aekpdf.app.core.ui.theme.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(
    viewModel: ImageToPdfViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (filePath: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddMoreSheet by remember { mutableStateOf(false) }

    // Drag-and-drop state tracking
    var draggingPageId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 140.dp.toPx() }

    // Multi-photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }

    // Google ML Kit Document Scanner Launcher (Automatic Edge Detection & Filters)
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val scannedUris = scanningResult?.pages?.map { it.imageUri } ?: emptyList()
            if (scannedUris.isNotEmpty()) {
                viewModel.addImages(scannedUris)
            }
        }
    }

    // PDF Document Picker Launcher (Mix existing PDF pages)
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.addPagesFromPdf(it) }
    }

    val launchAiScanner: () -> Unit = {
        if (activity != null) {
            DocumentScannerHelper.startScan(
                activity = activity,
                onSuccess = { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                },
                onFailure = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        } else {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    Scaffold(
        containerColor = AppleCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_image_to_pdf),
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = AppleTextPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.pages.isNotEmpty()) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(R.string.pdf_options_title),
                                tint = AppleBlue
                            )
                        }
                        IconButton(onClick = { viewModel.clearAll() }) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.action_clear_all),
                                tint = AppleRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleCanvas
                )
            )
        },
        bottomBar = {
            if (uiState.pages.isNotEmpty()) {
                Surface(
                    color = AppleCardSurface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(0.5.dp, AppleHairlineBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Estimated size reassurance
                        val estimatedMb = String.format(Locale.getDefault(), "%.1f", uiState.pages.size * 0.35)
                        Text(
                            text = "ขนาดประมาณ ~$estimatedMb MB • ส่ง LINE ได้เร็ว ไม่เปลืองเน็ต",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppleTextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Secondary: Add More Pages (Apple Tinted Button)
                            OutlinedButton(
                                onClick = { showAddMoreSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = AppleBlueTint,
                                    contentColor = AppleBlue
                                ),
                                border = BorderStroke(1.dp, AppleBlue.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "เพิ่มหน้า",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Primary: Create PDF (Apple Vibrant Prominent Button)
                            Button(
                                onClick = {
                                    viewModel.generatePdf { generatedFile ->
                                        onNavigateToPreview(generatedFile.absolutePath)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AppleBlue,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "สร้าง PDF (${uiState.pages.size} หน้า)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.pages.isEmpty()) {
                // Friendly Apple-styled Empty State
                EmptySelectionState(
                    onScanWithAi = launchAiScanner,
                    onPickImages = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickPdf = {
                        pdfPickerLauncher.launch(arrayOf("application/pdf"))
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Thai Name Presets Card (Apple Capsule Pills)
                    item(key = "thai_preset_card") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleCard(cornerRadius = 18.dp, elevation = 2.dp)
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ชื่อไฟล์: ${uiState.config.fileName}.pdf",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AppleTextPrimary,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "แก้ไข",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppleBlue,
                                        modifier = Modifier
                                            .clickable { showSettingsSheet = true }
                                            .padding(4.dp)
                                    )
                                }

                                Text(
                                    text = "แตะตั้งชื่อด่วน (ไม่ต้องพิมพ์เอง):",
                                    fontSize = 12.sp,
                                    color = AppleTextSecondary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val presets = listOf("บัตรประชาชน", "ใบเสร็จ", "เอกสารสัญญา", "ใบรับรองแพทย์", "เอกสารส่งงาน")
                                    presets.forEach { preset ->
                                        // Apple Capsule Pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(AppleBlueTint)
                                                .border(0.5.dp, AppleBlue.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                                                .clickable { viewModel.applyThaiNamePreset(preset) }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = preset,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppleBlue
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Friendly Helper Hint: Swipe to Delete & Drag to Reorder
                    item(key = "drag_swipe_hint") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "สไลด์ซ้ายเพื่อลบ • ลากไอคอนเพื่อสลับหน้า",
                                fontSize = 12.sp,
                                color = AppleTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Pages List with Thumbnails, Swipe to Delete & Drag-to-Reorder
                    itemsIndexed(
                        items = uiState.pages,
                        key = { _, item -> item.id }
                    ) { index, page ->
                        val isDragging = draggingPageId == page.id

                        PageListItem(
                            pageId = page.id,
                            pageIndex = index,
                            totalPages = uiState.pages.size,
                            uri = page.uri,
                            rotationDegrees = page.rotationDegrees,
                            isDragging = isDragging,
                            dragOffsetY = if (isDragging) dragOffsetY else 0f,
                            onRotate = { viewModel.rotatePage(index) },
                            onDelete = { viewModel.removePage(index) },
                            onDragStart = {
                                draggingPageId = page.id
                                dragOffsetY = 0f
                            },
                            onDrag = { dy ->
                                dragOffsetY += dy
                                val currentIdx = uiState.pages.indexOfFirst { it.id == draggingPageId }
                                if (currentIdx != -1) {
                                    if (dragOffsetY > itemHeightPx * 0.5f && currentIdx < uiState.pages.size - 1) {
                                        viewModel.movePage(currentIdx, currentIdx + 1)
                                        dragOffsetY -= itemHeightPx
                                    } else if (dragOffsetY < -itemHeightPx * 0.5f && currentIdx > 0) {
                                        viewModel.movePage(currentIdx, currentIdx - 1)
                                        dragOffsetY += itemHeightPx
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingPageId = null
                                dragOffsetY = 0f
                            }
                        )
                    }
                }
            }

            // Progress overlay during generation
            if (uiState.isGenerating) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = {
                        Text(
                            text = "กำลังสร้างไฟล์ PDF...",
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                progress = {
                                    if (uiState.totalProgressPages > 0) {
                                        uiState.currentProgressPage.toFloat() / uiState.totalProgressPages.toFloat()
                                    } else 0f
                                },
                                color = AppleBlue,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ประมวลผลหน้า ${uiState.currentProgressPage} จาก ${uiState.totalProgressPages}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppleTextSecondary
                            )
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = AppleCardSurface
                )
            }

            // Error Dialog
            uiState.errorMessage?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearAll() },
                    title = { Text("แจ้งเตือน", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
                    text = { Text(error, color = AppleTextSecondary) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearAll() },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ตกลง")
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = AppleCardSurface
                )
            }

            // Add More Options Bottom Sheet
            if (showAddMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAddMoreSheet = false },
                    containerColor = AppleCardSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .size(width = 36.dp, height = 5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleDivider)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "เพิ่มหน้าเอกสาร",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleTextPrimary
                        )

                        // AI Scanner Option
                        Button(
                            onClick = {
                                showAddMoreSheet = false
                                launchAiScanner()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppleIndigo)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ถ่ายสแกนด้วย AI (กล้อง)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // Photo Picker Option
                        OutlinedButton(
                            onClick = {
                                showAddMoreSheet = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppleBlueTint,
                                contentColor = AppleBlue
                            ),
                            border = BorderStroke(1.dp, AppleBlue.copy(alpha = 0.25f))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("เลือกรูปภาพจากอัลบั้ม", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        // PDF Picker Option
                        TextButton(
                            onClick = {
                                showAddMoreSheet = false
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AppleTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("หรือเพิ่มจากไฟล์ PDF เดิม", color = AppleTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            // PDF Settings Bottom Sheet
            if (showSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSettingsSheet = false },
                    containerColor = AppleCardSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .size(width = 36.dp, height = 5.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppleDivider)
                        )
                    }
                ) {
                    PdfSettingsContent(
                        config = uiState.config,
                        onUpdateFileName = viewModel::updateFileName,
                        onUpdatePageFormat = viewModel::updatePageFormat,
                        onUpdateQuality = viewModel::updateQuality,
                        onUpdateFilter = viewModel::updateFilter,
                        onDismiss = { showSettingsSheet = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageListItem(
    pageId: String,
    pageIndex: Int,
    totalPages: Int,
    uri: Uri,
    rotationDegrees: Int,
    isDragging: Boolean,
    dragOffsetY: Float,
    onRotate: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val currentOnRotate by rememberUpdatedState(onRotate)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    val elevation by animateDpAsState(if (isDragging) 8.dp else 2.dp, label = "elevation")
    val scale by animateFloatAsState(if (isDragging) 1.03f else 1f, label = "scale")

    // Swipe-to-Dismiss Box (Slide left to delete)
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) AppleRed else AppleRed.copy(alpha = 0.85f),
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .clickable { currentOnDelete() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "ลบหน้านี้",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) "ปล่อยเพื่อลบ" else "ลบหน้านี้",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    ) {
        // Elevated Pure White Card Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    if (isDragging) {
                        translationY = dragOffsetY
                        scaleX = scale
                        scaleY = scale
                    }
                }
                .appleCard(cornerRadius = 16.dp, elevation = elevation)
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Document Page Thumbnail with Badge
                Box(
                    modifier = Modifier
                        .size(width = 76.dp, height = 102.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleCanvas)
                        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(10.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Page thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationDegrees.toFloat())
                    )

                    // Page badge pill
                    Box(
                        modifier = Modifier
                            .padding(6.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${pageIndex + 1}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Middle: Page Title and Rotate 90° button
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "หน้าที่ ${pageIndex + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = AppleTextPrimary
                    )

                    // Rotate 90° Button (Apple Blue Tinted Pill)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppleBlueTint)
                            .clickable(onClick = currentOnRotate)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "หมุนรูป",
                                tint = AppleBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "หมุน 90°",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleBlue
                            )
                        }
                    }
                }

                // Right: Dedicated Apple Reorder / Drag Handle (Instant touch drag)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDragging) AppleBlueTint else AppleCanvas)
                        .pointerInput(pageId) {
                            try {
                                detectDragGestures(
                                    onDragStart = { currentOnDragStart() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentOnDrag(dragAmount.y)
                                    },
                                    onDragEnd = { currentOnDragEnd() },
                                    onDragCancel = { currentOnDragEnd() }
                                )
                            } finally {
                                currentOnDragEnd()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "ลากเพื่อสลับตำแหน่ง",
                        tint = if (isDragging) AppleBlue else AppleTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySelectionState(
    onScanWithAi: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdf: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Hero Card in Empty State
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .appleCard(cornerRadius = 24.dp, elevation = 3.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Indigo/Blue Scanner Squircle Badge
                Box(
                    modifier = Modifier.appleSquircleGradientBadge(
                        colors = listOf(AppleIndigo, AppleBlue),
                        size = 68.dp,
                        cornerRadius = 18.dp
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "รวมเอกสารเป็นไฟล์ PDF",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "เลือกวิธีเพิ่มเอกสารสแกนที่คุณพ่อต้องการ\nไร้ลายน้ำ 100% • ปลอดภัยในเครื่อง ไม่เสียเงิน",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = AppleTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Option: AI Scanner
                Button(
                    onClick = onScanWithAi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ถ่ายสแกนด้วย AI (ตัดขอบให้อัตโนมัติ)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Secondary Option: Photo Album
                OutlinedButton(
                    onClick = onPickImages,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = AppleBlueTint,
                        contentColor = AppleBlue
                    ),
                    border = BorderStroke(1.dp, AppleBlue.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "เลือกรูปภาพจากอัลบั้ม",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tertiary Option: Combine existing PDF
                TextButton(
                    onClick = onPickPdf,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AppleTextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "หรือเพิ่มจากไฟล์ PDF เดิมที่มีอยู่แล้ว",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AppleTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun PdfSettingsContent(
    config: com.aekpdf.app.core.model.PdfExportConfig,
    onUpdateFileName: (String) -> Unit,
    onUpdatePageFormat: (PageFormat) -> Unit,
    onUpdateQuality: (ExportQuality) -> Unit,
    onUpdateFilter: (PageFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ตั้งค่าไฟล์ PDF",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = AppleTextPrimary
        )

        // File name input
        OutlinedTextField(
            value = config.fileName,
            onValueChange = onUpdateFileName,
            label = { Text("ชื่อไฟล์ PDF") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppleBlue,
                unfocusedBorderColor = AppleDivider
            )
        )

        // Page Format Selector
        Text(text = "ขนาดหน้ากระดาษ", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppleTextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageFormat.values().forEach { format ->
                FilterChip(
                    selected = config.pageFormat == format,
                    onClick = { onUpdatePageFormat(format) },
                    label = { Text(stringResource(format.displayNameRes)) },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleBlueTint,
                        selectedLabelColor = AppleBlue
                    )
                )
            }
        }

        // Quality Selector
        Text(text = "คุณภาพและความคมชัด", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppleTextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExportQuality.values().forEach { quality ->
                FilterChip(
                    selected = config.quality == quality,
                    onClick = { onUpdateQuality(quality) },
                    label = { Text(stringResource(quality.displayNameRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleBlueTint,
                        selectedLabelColor = AppleBlue
                    )
                )
            }
        }

        // Color Filter Selector
        Text(text = "สีเอกสาร", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = AppleTextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageFilter.values().forEach { filter ->
                FilterChip(
                    selected = config.filter == filter,
                    onClick = { onUpdateFilter(filter) },
                    label = { Text(stringResource(filter.displayNameRes)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppleBlueTint,
                        selectedLabelColor = AppleBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
        ) {
            Text("เสร็จสิ้น", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
