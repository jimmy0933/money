package com.moneytracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 中文字不需要額外字距，letterSpacing 一律 0，行高固定，避免不同大小文字基線錯位。
private fun style(size: Int, weight: FontWeight, line: Int) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = 0.sp
)

val Typography = Typography(
    headlineMedium = style(28, FontWeight.Bold, 36),
    headlineSmall = style(24, FontWeight.Bold, 32),
    titleLarge = style(22, FontWeight.Bold, 28),
    titleMedium = style(16, FontWeight.SemiBold, 24),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(14, FontWeight.Medium, 20),
    labelMedium = style(12, FontWeight.Medium, 16),
    labelSmall = style(11, FontWeight.Medium, 16)
)
