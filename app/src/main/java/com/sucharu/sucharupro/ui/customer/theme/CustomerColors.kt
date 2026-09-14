package com.sucharu.sucharupro.ui.customer.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mobile-First Design System Color Tokens for Sucharu Pro Customer & Affiliate Experiences.
 *
 * Adheres to the approved visual identity:
 * - Premium dark navy foundation
 * - Personal, friendly, trustworthy, fast aesthetic
 * - Vibrant semantic accent colors for offers, orders, and wallet activities
 */
@Immutable
data class CustomerColors(
    val background: Color = Color(0xFF090E17),
    val surface: Color = Color(0xFF131D2E),
    val elevatedSurface: Color = Color(0xFF1E2A3E),
    val border: Color = Color(0xFF2A3B53),
    val primaryText: Color = Color(0xFFFFFFFF),
    val secondaryText: Color = Color(0xFF90A4AE),
    val mutedText: Color = Color(0xFF607D8B),
    val accentPrimary: Color = Color(0xFF00E5FF),    // Neon Cyan
    val accentPurple: Color = Color(0xFF7C4DFF),     // Electric Purple
    val accentAmber: Color = Color(0xFFFFD600),      // Warm Amber
    val success: Color = Color(0xFF00E676),          // Emerald Green
    val warning: Color = Color(0xFFFF9100),          // Neon Orange
    val error: Color = Color(0xFFFF5252),            // Coral Red
    val info: Color = Color(0xFF29B6F6),             // Electric Blue
    val successContainer: Color = Color(0xFF00381A),
    val warningContainer: Color = Color(0xFF3B2200),
    val errorContainer: Color = Color(0xFF3B0A0A),
    val infoContainer: Color = Color(0xFF0A2B3B),
    val accentContainer: Color = Color(0xFF003340)
)

val LocalCustomerColors = staticCompositionLocalOf { CustomerColors() }
