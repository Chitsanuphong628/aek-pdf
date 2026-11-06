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
    onNavigateToImageToPdf: () -> Unit,
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
                                text = "ไร้ลายน้ำ 100% • ปลอดภัยในเครื่อง",
                                fontSize = 11.sp,
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
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Hero Feature: Image to PDF (Apple Card Style)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleCard(cornerRadius = 22.dp, elevation = 3.dp)
                        .clickable(onClick = onNavigateToImageToPdf)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Vibrant Squircle App Icon Badge
                            Box(
                                modifier = Modifier.appleSquircleGradientBadge(
                                    colors = listOf(AppleBlue, Color(0xFF32ADE6)),
                                    size = 56.dp,
                                    cornerRadius = 14.dp
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            // Apple Pill: Free & Watermark-Free
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(AppleGreenTint)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ฟรี • ไร้ลายน้ำ 100%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppleGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "แปลงรูปเป็น PDF (Images to PDF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = AppleTextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "เลือกรูปภาพเอกสารสแกนที่คุณพ่อถ่ายไว้ รวมเป็นไฟล์ PDF ฉบับเดียวอย่างรวดเร็ว จัดหน้าและหมุนรูปได้ทันที",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = AppleTextSecondary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Apple Prominent Action Button
                        Button(
                            onClick = onNavigateToImageToPdf,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppleBlue,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "เริ่มรวมไฟล์เอกสาร",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
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
                        // Vibrant Orange Squircle Badge
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
                                text = "เปิดดูหรือแชร์ส่งต่อให้ผู้อื่นได้ทันที",
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

            // Future Tools Section (Apple-style Grouped Tools)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "เครื่องมือที่จะมาในอนาคต (Roadmap)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleTextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FutureToolAppleCard(
                            icon = Icons.AutoMirrored.Filled.CallMerge,
                            badgeColor = AppleIndigo,
                            title = "รวมไฟล์ PDF หลายฉบับ (PDF Merge)",
                            description = "นำไฟล์ PDF สองฉบับขึ้นไปมารวมเป็นเอกสารเดียว"
                        )

                        FutureToolAppleCard(
                            icon = Icons.Default.Compress,
                            badgeColor = AppleOrange,
                            title = "ย่อขนาดไฟล์ PDF (PDF Compress)",
                            description = "ลดขนาดไฟล์เอกสารเพื่อให้ส่งอีเมลหรือไลน์ได้สะดวกยิ่งขึ้น"
                        )

                        FutureToolAppleCard(
                            icon = Icons.Default.CameraAlt,
                            badgeColor = AppleTeal,
                            title = "สแกนเอกสารด้วยกล้อง (Camera Scanner)",
                            description = "สแกนจากกล้องพร้อมตัดขอบเอกสารอัตโนมัติ"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FutureToolAppleCard(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .appleCard(cornerRadius = 16.dp, elevation = 1.dp)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.appleSquircleBadge(
                    color = badgeColor,
                    size = 40.dp,
                    cornerRadius = 10.dp
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AppleTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = AppleTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppleCanvas)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "เร็วๆ นี้",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppleTextSecondary
                )
            }
        }
    }
}
