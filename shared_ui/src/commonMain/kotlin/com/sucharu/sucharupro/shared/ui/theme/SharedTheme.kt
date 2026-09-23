package com.sucharu.sucharupro.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.sucharu.sucharupro.shared_ui.resources.Res
import com.sucharu.sucharupro.shared_ui.resources.noto_sans_bengali_regular
import org.jetbrains.compose.resources.Font

val SharedDarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFFD97706),
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B)
)

val SharedLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    secondary = Color(0xFFD97706),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF)
)

/**
 * Shared Compose Multiplatform Theme for Sucharu Pro (Android & Web/Desktop).
 */
@Composable
fun SharedTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SharedDarkColorScheme else SharedLightColorScheme
    val bengaliFontFamily = FontFamily(Font(Res.font.noto_sans_bengali_regular))

    val defaultTypography = Typography()
    val bengaliTypography = Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = bengaliFontFamily),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = bengaliFontFamily),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = bengaliFontFamily),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = bengaliFontFamily),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = bengaliFontFamily),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = bengaliFontFamily),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = bengaliFontFamily),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = bengaliFontFamily),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = bengaliFontFamily),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = bengaliFontFamily),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = bengaliFontFamily),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = bengaliFontFamily),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = bengaliFontFamily),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = bengaliFontFamily),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = bengaliFontFamily)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = bengaliTypography,
        content = content
    )
}
