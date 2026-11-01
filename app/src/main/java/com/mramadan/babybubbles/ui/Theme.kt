package com.mramadan.babybubbles.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mramadan.babybubbles.R

object Palette {
    val Purple = Color(0xFF6A3DE8)
    val Ink = Color(0xFF2B2550)
    val Muted = Color(0xFF5B5677)
    val Lavender = Color(0xFFF7F4FF)
    val CardBorder = Color(0xFFE6E0F7)
    val Track = Color(0xFFF1EDFD)
    val Divider = Color(0xFFEEEAF8)
    val TipBackground = Color(0xFFFFF1DC)
    val TipInk = Color(0xFF6B3A00)
    val TipIcon = Color(0xFFA85A00)
    val Scrim = Color(0xFF2B2550)
    val AnswerBorder = Color(0xFFE3DDF7)
    val ProgressTrack = Color(0xFFEEE9FB)
}

/** Baloo Bhaijaan 2 (SIL Open Font License), static instances of the variable font. */
val Baloo = FontFamily(
    Font(R.font.baloo_semibold, FontWeight.Normal),
    Font(R.font.baloo_semibold, FontWeight.Medium),
    Font(R.font.baloo_semibold, FontWeight.SemiBold),
    Font(R.font.baloo_bold, FontWeight.Bold),
    Font(R.font.baloo_extrabold, FontWeight.ExtraBold),
)

private fun Typography.withFont(f: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = f),
    displayMedium = displayMedium.copy(fontFamily = f),
    displaySmall = displaySmall.copy(fontFamily = f),
    headlineLarge = headlineLarge.copy(fontFamily = f),
    headlineMedium = headlineMedium.copy(fontFamily = f),
    headlineSmall = headlineSmall.copy(fontFamily = f),
    titleLarge = titleLarge.copy(fontFamily = f),
    titleMedium = titleMedium.copy(fontFamily = f),
    titleSmall = titleSmall.copy(fontFamily = f),
    bodyLarge = bodyLarge.copy(fontFamily = f),
    bodyMedium = bodyMedium.copy(fontFamily = f),
    bodySmall = bodySmall.copy(fontFamily = f),
    labelLarge = labelLarge.copy(fontFamily = f),
    labelMedium = labelMedium.copy(fontFamily = f),
    labelSmall = labelSmall.copy(fontFamily = f),
)

@Composable
fun BabyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Palette.Purple,
            onPrimary = Color.White,
            background = Palette.Lavender,
            onBackground = Palette.Ink,
            surface = Color.White,
            onSurface = Palette.Ink,
        ),
        typography = Typography().withFont(Baloo),
        content = content,
    )
}
