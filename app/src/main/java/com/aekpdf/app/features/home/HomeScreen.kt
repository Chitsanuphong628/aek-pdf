package com.aekpdf.app.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aekpdf.app.R
import com.aekpdf.app.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLaunchRapidCamera: () -> Unit,
    onScanWithCamera: () -> Unit,
    onPickImages: () -> Unit,
    onPickPdfs: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    Scaffold(
        containerColor = AppleCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.appleSquircleGradientBadge(
                                colors = listOf(AppleBlue, Color(0xFF32ADE6)),
                                size = 38.dp,
                                cornerRadius = 10.dp
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.title_home),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = AppleTextPrimary
                            )
                            Text(
                                text = "จัดการเอกสาร PDF ออฟไลน์",
                                fontSize = 12.sp,
                                color = AppleTextSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(AppleCardSurface)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(R.string.title_history),
                            tint = AppleBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppleCanvas
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Tools Section Header
            item {
                Text(
                    text = "เครื่องมือจัดการเอกสาร",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            // Tool 1: Rapid Camera Scanner (Primary action)
            item {
                HomeToolCard(
                    icon = Icons.Default.CameraAlt,
                    badgeColors = listOf(AppleIndigo, AppleBlue),
                    title = "ถ่ายรัวต่อเนื่อง (หลายหน้า)",
                    description = "ถ่ายติดกันรวดเร็ว ไม่ต้องรอกดทีละหน้า แล้วรวมเป็น PDF ทันที",
                    isPrimary = true,
                    onClick = onLaunchRapidCamera
                )
            }

            // Tool 2: Automatic Edge Scanner (ML Kit)
            item {
                HomeToolCard(
                    icon = Icons.Default.DocumentScanner,
                    badgeColor = AppleTeal,
                    title = "สแกนตัดขอบอัตโนมัติ",
                    description = "ตรวจจับขอบกระดาษและปรับมุมภาพทีละหน้า",
                    isPrimary = false,
                    onClick = onScanWithCamera
                )
            }

            // Tool 3: Images to PDF
            item {
                HomeToolCard(
                    icon = Icons.Default.AddPhotoAlternate,
                    badgeColor = AppleBlue,
                    title = "แปลงรูปภาพเป็น PDF",
                    description = "เลือกรูปถ่ายจากอัลบั้มเพื่อสร้างเป็นเอกสาร",
                    isPrimary = false,
                    onClick = onPickImages
                )
            }

            // Tool 4: PDF Merge
            item {
                HomeToolCard(
                    icon = Icons.AutoMirrored.Filled.CallMerge,
                    badgeColor = ApplePurple,
                    title = "รวมไฟล์ PDF หลายฉบับ",
                    description = "เลือกไฟล์ PDF เดิมมาจัดหน้าและรวมเป็นฉบับเดียว",
                    isPrimary = false,
                    onClick = onPickPdfs
                )
            }

            // History Section Header
            item {
                Text(
                    text = "ประวัติและไฟล์ล่าสุด",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppleTextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Quick Access: Document History Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleCard(cornerRadius = 18.dp, elevation = 2.dp)
                        .clickable(onClick = onNavigateToHistory)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.appleSquircleBadge(
                                color = AppleOrange,
                                size = 44.dp,
                                cornerRadius = 12.dp
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ประวัติเอกสารที่สร้างแล้ว",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AppleTextPrimary
                            )
                            Text(
                                text = "ดูและแชร์ไฟล์ PDF ที่เคยบันทึกไว้",
                                fontSize = 13.sp,
                                color = AppleTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AppleTextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeToolCard(
    icon: ImageVector,
    badgeColor: Color = AppleBlue,
    badgeColors: List<Color>? = null,
    title: String,
    description: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appleCard(
                cornerRadius = if (isPrimary) 20.dp else 18.dp,
                elevation = if (isPrimary) 3.dp else 2.dp
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Squircle Badge
            Box(
                modifier = if (badgeColors != null) {
                    Modifier.appleSquircleGradientBadge(
                        colors = badgeColors,
                        size = 50.dp,
                        cornerRadius = 14.dp
                    )
                } else {
                    Modifier.appleSquircleBadge(
                        color = badgeColor,
                        size = 50.dp,
                        cornerRadius = 14.dp
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppleTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = AppleTextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AppleCanvas),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = AppleTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
