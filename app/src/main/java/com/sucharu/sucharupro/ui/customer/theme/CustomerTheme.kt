package com.sucharu.sucharupro.ui.customer.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val CustomerDarkColorScheme = darkColorScheme(
    primary = ColorDefaults.colors.accentPrimary,
    onPrimary = ColorDefaults.colors.background,
    primaryContainer = ColorDefaults.colors.elevatedSurface,
    onPrimaryContainer = ColorDefaults.colors.primaryText,
    secondary = ColorDefaults.colors.accentPurple,
    onSecondary = ColorDefaults.colors.primaryText,
    background = ColorDefaults.colors.background,
    onBackground = ColorDefaults.colors.primaryText,
    surface = ColorDefaults.colors.surface,
    onSurface = ColorDefaults.colors.primaryText,
    surfaceVariant = ColorDefaults.colors.elevatedSurface,
    onSurfaceVariant = ColorDefaults.colors.secondaryText,
    outline = ColorDefaults.colors.border,
    error = ColorDefaults.colors.error,
    onError = ColorDefaults.colors.primaryText
)

private object ColorDefaults {
    val colors = CustomerColors()
}

/**
 * Accessor object for Sucharu Pro Customer Theme tokens.
 */
object CustomerTheme {
    val colors: CustomerColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomerColors.current

    val typography: CustomerTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomerTypography.current

    val spacing: CustomerSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalCustomerSpacing.current
}

/**
 * Mobile-First Theme Wrapper for Sucharu Pro Customer & Affiliate User Experiences.
 */
@Composable
fun CustomerTheme(
    colors: CustomerColors = CustomerColors(),
    typography: CustomerTypography = CustomerTypography(),
    spacing: CustomerSpacing = CustomerSpacing(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalCustomerColors provides colors,
        LocalCustomerTypography provides typography,
        LocalCustomerSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = CustomerDarkColorScheme,
            content = content
        )
    }
}
