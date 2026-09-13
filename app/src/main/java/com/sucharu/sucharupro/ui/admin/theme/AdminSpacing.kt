package com.sucharu.sucharupro.ui.admin.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive Grid & Spacing System for Sucharu Pro Admin Panel.
 *
 * Provides standardized spacing metrics and component metrics for adaptive layouts.
 */
@Immutable
data class AdminSpacing(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,

    // Component-level standard metrics
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    val cardCornerRadius: Dp = 16.dp,
    val dialogCornerRadius: Dp = 20.dp,
    val badgeCornerRadius: Dp = 8.dp,
    val chipCornerRadius: Dp = 20.dp,
    val buttonMinHeight: Dp = 48.dp, // Touch-friendly accessibility target
    val touchTargetMin: Dp = 48.dp
)

/**
 * Window width size classifications for responsive Admin layouts.
 */
enum class AdminWindowSizeClass {
    COMPACT,   // Phone (< 600dp)
    MEDIUM,    // Tablet (600dp - 840dp)
    EXPANDED   // Desktop / Wide Tablet (> 840dp)
}

val LocalAdminSpacing = staticCompositionLocalOf { AdminSpacing() }
