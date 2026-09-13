package com.sucharu.sucharupro.ui.admin.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Authoritative Dark-Mode-First Color Token System for Sucharu Pro Admin Panel.
 *
 * Adheres to the approved design direction:
 * - Premium dark navy / near-black foundation
 * - Elevated dark surfaces with subtle slate borders
 * - Vibrant semantic accent and status colors
 */
@Immutable
data class AdminColors(
    val background: Color = Color(0xFF090E17),
    val surface: Color = Color(0xFF131D2E),
    val elevatedSurface: Color = Color(0xFF1E2A3E),
    val border: Color = Color(0xFF2A3B53),
    val primaryText: Color = Color(0xFFFFFFFF),
    val secondaryText: Color = Color(0xFF90A4AE),
    val mutedText: Color = Color(0xFF607D8B),
    val accentPrimary: Color = Color(0xFF00E5FF),   // Neon Cyan
    val accentPurple: Color = Color(0xFF7C4DFF),    // Electric Purple
    val accentAmber: Color = Color(0xFFFFD600),     // Warm Amber
    val success: Color = Color(0xFF00E676),         // Emerald Green
    val warning: Color = Color(0xFFFF9100),         // Neon Orange
    val error: Color = Color(0xFFFF5252),           // Coral Red
    val info: Color = Color(0xFF29B6F6),            // Electric Sky Blue
    val successContainer: Color = Color(0xFF00381A),
    val warningContainer: Color = Color(0xFF3B2200),
    val errorContainer: Color = Color(0xFF3B0A0A),
    val infoContainer: Color = Color(0xFF0A2B3B),
    val accentContainer: Color = Color(0xFF003340)
)

val LocalAdminColors = staticCompositionLocalOf { AdminColors() }
