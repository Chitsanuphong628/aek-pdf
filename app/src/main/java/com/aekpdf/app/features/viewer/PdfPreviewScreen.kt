package com.aekpdf.app.features.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aekpdf.app.R
import com.aekpdf.app.core.storage.PdfStorageManager
import com.aekpdf.app.core.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    filePath: String,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Render current page bitmap safely in background
    LaunchedEffect(file, currentPageIndex) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                if (file.exists() && file.length() > 0L) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            val count = renderer.pageCount
                            withContext(Dispatchers.Main) {
                                totalPages = count
                            }
                            if (count > 0 && currentPageIndex < count) {
                                val page = renderer.openPage(currentPageIndex)
                                val targetWidth = (page.width * 2).coerceAtMost(2048)
                                val targetHeight = (page.height * 2).coerceAtMost(2048)
                                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                page.close()
                                withContext(Dispatchers.Main) {
                                    pageBitmap = bitmap
                                    isLoading = false
                                }
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        containerColor = AppleCanvas,
        topBar = {
            TopAppBar(
                title = { Text("ตรวจทานเอกสาร PDF", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ", tint = AppleTextPrimary)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateHome,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(AppleCardSurface)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "หน้าหลัก", tint = AppleBlue)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppleCanvas)
            )
        },
        bottomBar = {
            Surface(
                color = AppleCardSurface,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, AppleHairlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Primary Action: Direct LINE Share
                    Button(
                        onClick = {
                            try {
                                val lineIntent = PdfStorageManager.createLineShareIntent(context, file)
                                context.startActivity(lineIntent)
                            } catch (_: Exception) {
                                val shareIntent = PdfStorageManager.createShareIntent(context, file)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "ส่งไฟล์ PDF ผ่าน"))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LineGreen
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ส่งเข้า LINE ทันที", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Secondary Actions: Open and Other Share (Apple Tinted Buttons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Open File
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = PdfStorageManager.createOpenIntent(context, file)
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppleBlueTint,
                                contentColor = AppleBlue
                            ),
                            border = BorderStroke(1.dp, AppleBlue.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("เปิดดูเอกสาร", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // Other Share
                        OutlinedButton(
                            onClick = {
                                val shareIntent = PdfStorageManager.createShareIntent(context, file)
                                context.startActivity(android.content.Intent.createChooser(shareIntent, "แชร์ไฟล์ PDF ผ่าน"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = AppleCanvas,
                                contentColor = AppleTextPrimary
                            ),
                            border = BorderStroke(0.5.dp, AppleHairlineBorder)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("แชร์อื่นๆ", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Success Info Banner (Apple Card Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .appleCard(cornerRadius = 18.dp, elevation = 2.dp)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vibrant Green Checkmark Squircle Badge
                    Box(
                        modifier = Modifier.appleSquircleBadge(
                            color = AppleGreen,
                            size = 40.dp,
                            cornerRadius = 12.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = file.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AppleTextPrimary
                        )
                        val sizeFormatted = formatFileSize(file.length())
                        Text(
                            text = "ขนาด: $sizeFormatted • ทั้งหมด $totalPages หน้า (ไร้ลายน้ำ 100%)",
                            fontSize = 13.sp,
                            color = AppleTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Page Navigation Selector (Apple Pill Segment)
            if (totalPages > 1) {
                Box(
                    modifier = Modifier
                        .appleCard(cornerRadius = 100.dp, elevation = 1.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "หน้าก่อน",
                                tint = if (currentPageIndex > 0) AppleTextPrimary else AppleTextTertiary
                            )
                        }
                        Text(
                            text = "หน้าที่ ${currentPageIndex + 1} จาก $totalPages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AppleTextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "หน้าถัดไป",
                                tint = if (currentPageIndex < totalPages - 1) AppleTextPrimary else AppleTextTertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Native PDF Page Preview Box (Desk Paper Simulation with Soft Shadow)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .appleCard(cornerRadius = 16.dp, elevation = 3.dp, backgroundColor = AppleCardSurface)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                pageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                } ?: run {
                    CircularProgressIndicator(color = AppleBlue)
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
