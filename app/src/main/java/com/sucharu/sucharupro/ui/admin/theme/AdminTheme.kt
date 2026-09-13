package com.sucharu.sucharupro.ui.admin.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val AdminDarkColorScheme = darkColorScheme(
    primary = ColorTokenDefaults.colors.accentPrimary,
    onPrimary = ColorTokenDefaults.colors.background,
    primaryContainer = ColorTokenDefaults.colors.elevatedSurface,
    onPrimaryContainer = ColorTokenDefaults.colors.primaryText,
    secondary = ColorTokenDefaults.colors.accentPurple,
    onSecondary = ColorTokenDefaults.colors.primaryText,
    background = ColorTokenDefaults.colors.background,
    onBackground = ColorTokenDefaults.colors.primaryText,
    surface = ColorTokenDefaults.colors.surface,
    onSurface = ColorTokenDefaults.colors.primaryText,
    surfaceVariant = ColorTokenDefaults.colors.elevatedSurface,
    onSurfaceVariant = ColorTokenDefaults.colors.secondaryText,
    outline = ColorTokenDefaults.colors.border,
    error = ColorTokenDefaults.colors.error,
    onError = ColorTokenDefaults.colors.primaryText
)

private object ColorTokenDefaults {
    val colors = AdminColors()
}

/**
 * Accessor object for Sucharu Pro Admin Theme tokens.
 */
object AdminTheme {
    val colors: AdminColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAdminColors.current

    val typography: AdminTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAdminTypography.current

    val spacing: AdminSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalAdminSpacing.current
}

/**
 * Dark-Mode-First Theme Wrapper for Sucharu Pro Admin Panel.
 */
@Composable
fun AdminTheme(
    colors: AdminColors = AdminColors(),
    typography: AdminTypography = AdminTypography(),
    spacing: AdminSpacing = AdminSpacing(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAdminColors provides colors,
        LocalAdminTypography provides typography,
        LocalAdminSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = AdminDarkColorScheme,
            content = content
        )
    }
}
