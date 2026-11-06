package com.aekpdf.app.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies Apple-style soft ambient elevation, subtle hairline border, and clean surface.
 */
fun Modifier.appleCard(
    cornerRadius: Dp = 18.dp,
    elevation: Dp = 2.dp,
    backgroundColor: Color = AppleCardSurface,
    borderColor: Color = AppleHairlineBorder
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color(0x0D000000),
            spotColor = Color(0x14000000)
        )
        .border(
            width = 0.5.dp,
            color = borderColor,
            shape = shape
        )
        .background(
            color = backgroundColor,
            shape = shape
        )
        .clip(shape)
}

fun Modifier.appleCardShape(
    shape: Shape,
    elevation: Dp = 2.dp,
    backgroundColor: Color = AppleCardSurface,
    borderColor: Color = AppleHairlineBorder
): Modifier = this
    .shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = Color(0x0D000000),
        spotColor = Color(0x14000000)
    )
    .border(
        width = 0.5.dp,
        color = borderColor,
        shape = shape
    )
    .background(
        color = backgroundColor,
        shape = shape
    )
    .clip(shape)

/**
 * Creates an iOS-style Squircle icon badge container with vibrant solid or gradient background.
 */
fun Modifier.appleSquircleBadge(
    color: Color,
    size: Dp = 44.dp,
    cornerRadius: Dp = 12.dp
): Modifier = this
    .size(size)
    .clip(RoundedCornerShape(cornerRadius))
    .background(color)

/**
 * Creates an iOS-style Squircle icon badge container with gradient.
 */
fun Modifier.appleSquircleGradientBadge(
    colors: List<Color>,
    size: Dp = 52.dp,
    cornerRadius: Dp = 14.dp
): Modifier = this
    .size(size)
    .clip(RoundedCornerShape(cornerRadius))
    .background(Brush.linearGradient(colors))
