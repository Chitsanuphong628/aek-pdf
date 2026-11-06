package com.aekpdf.app.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aekpdf.app.R
import com.aekpdf.app.core.database.PdfHistoryEntity
import com.aekpdf.app.core.storage.PdfStorageManager
import com.aekpdf.app.core.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onSelectPdf: (filePath: String) -> Unit
) {
    val historyList by viewModel.historyList.collectAsState()
    val context = LocalContext.current
    var itemToDelete by remember { mutableStateOf<PdfHistoryEntity?>(null) }

    Scaffold(
        containerColor = AppleCanvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_history), fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ย้อนกลับ", tint = AppleTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppleCanvas)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (historyList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.appleSquircleBadge(
                            color = AppleOrangeTint,
                            size = 80.dp,
                            cornerRadius = 20.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = AppleOrange
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.prompt_empty_history),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppleTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ไฟล์ PDF ที่คุณสร้างจะปรากฏที่นี่",
                        fontSize = 13.sp,
                        color = AppleTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(historyList, key = { it.id }) { item ->
                        HistoryItemAppleCard(
                            item = item,
                            onClick = { onSelectPdf(item.filePath) },
                            onShare = {
                                val file = File(item.filePath)
                                if (file.exists()) {
                                    val intent = PdfStorageManager.createShareIntent(context, file)
                                    context.startActivity(android.content.Intent.createChooser(intent, "แชร์ไฟล์ PDF"))
                                }
                            },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }

            // Confirm Delete Dialog (Apple Style)
            itemToDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { itemToDelete = null },
                    title = { Text("ยืนยันการลบ", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
                    text = { Text("ต้องการลบไฟล์ \"${item.title}\" ออกจากประวัติใช่หรือไม่?", color = AppleTextSecondary) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteHistory(item)
                                itemToDelete = null
                            }
                        ) {
                            Text("ลบ", color = AppleRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemToDelete = null }) {
                            Text("ยกเลิก", color = AppleTextSecondary)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = AppleCardSurface
                )
            }
        }
    }
}

@Composable
fun HistoryItemAppleCard(
    item: PdfHistoryEntity,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(item.createdAt) {
        val sdf = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(item.createdAt))
    }

    val sizeStr = remember(item.fileSizeBytes) {
        when {
            item.fileSizeBytes >= 1024 * 1024 -> String.format(Locale.getDefault(), "%.1f MB", item.fileSizeBytes.toDouble() / (1024 * 1024))
            item.fileSizeBytes >= 1024 -> String.format(Locale.getDefault(), "%.1f KB", item.fileSizeBytes.toDouble() / 1024)
            else -> "${item.fileSizeBytes} B"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appleCard(cornerRadius = 16.dp, elevation = 2.dp)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apple Red PDF Squircle Badge
            Box(
                modifier = Modifier.appleSquircleBadge(
                    color = AppleRed,
                    size = 44.dp,
                    cornerRadius = 12.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = AppleTextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$dateStr • ${item.pageCount} หน้า • $sizeStr",
                    fontSize = 12.sp,
                    color = AppleTextSecondary
                )
            }

            // Share Action (Apple Blue Tint Circle)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppleBlueTint)
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "แชร์",
                    tint = AppleBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Action (Apple Red Tint Circle)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppleRedTint)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "ลบ",
                    tint = AppleRed,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
