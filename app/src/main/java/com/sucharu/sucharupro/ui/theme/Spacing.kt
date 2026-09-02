package com.sucharu.sucharupro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class SucharuSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val xxLarge: Dp = 32.dp,
    val xxxLarge: Dp = 48.dp,
    
    // Component specific standard metrics
    val cardPadding: Dp = 16.dp,
    val screenPadding: Dp = 16.dp,
    val buttonHeight: Dp = 50.dp,
    val inputHeight: Dp = 56.dp,
    val touchTargetMin: Dp = 48.dp
)

val LocalSpacing = staticCompositionLocalOf { SucharuSpacing() }

val MaterialTheme.spacing: SucharuSpacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
