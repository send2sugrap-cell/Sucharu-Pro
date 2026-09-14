package com.sucharu.sucharupro.ui.customer.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Mobile-First Design System Color Tokens for Sucharu Pro Customer & Affiliate Experiences.
 *
 * Supports both dark navy foundation and light/neutral modern aesthetic for front-facing Home/Wall.
 */
@Immutable
data class CustomerColors(
    val isLight: Boolean = false,
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
) {
    companion object {
        fun light() = CustomerColors(
            isLight = true,
            background = Color(0xFFF8FAFC),        // Clean Light Slate Background
            surface = Color(0xFFFFFFFF),           // Crisp White Card Surface
            elevatedSurface = Color(0xFFF1F5F9),   // Soft Gray Surface
            border = Color(0xFFE2E8F0),            // Subtle Slate Border
            primaryText = Color(0xFF0F172A),       // Deep Slate Primary Text
            secondaryText = Color(0xFF64748B),     // Muted Slate Text
            mutedText = Color(0xFF94A3B8),         // Light Muted Text
            accentPrimary = Color(0xFF0284C7),     // Sky Cyan Blue Accent
            accentPurple = Color(0xFF7C3AED),      // Electric Purple Accent
            accentAmber = Color(0xFFD97706),       // Amber Accent
            success = Color(0xFF16A34A),           // Emerald Green
            warning = Color(0xFFD97706),           // Amber Orange
            error = Color(0xFFDC2626),             // Coral Red
            info = Color(0xFF0284C7),              // Electric Blue
            successContainer = Color(0xFFDCFCE7),  // Soft Green Container
            warningContainer = Color(0xFFFEF3C7),  // Soft Yellow Container
            errorContainer = Color(0xFFFEE2E2),    // Soft Red Container
            infoContainer = Color(0xFFE0F2FE),     // Soft Blue Container
            accentContainer = Color(0xFFE0F2FE)    // Soft Accent Container
        )
    }
}

val LocalCustomerColors = staticCompositionLocalOf { CustomerColors() }
