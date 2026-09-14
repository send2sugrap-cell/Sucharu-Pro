package com.sucharu.sucharupro.ui.customer.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mobile-First Spacing and Metrics System for Customer & Affiliate Experiences.
 */
@Immutable
data class CustomerSpacing(
    val none: Dp = 0.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,

    // Component metrics
    val cardCornerRadius: Dp = 16.dp,
    val offerCardCornerRadius: Dp = 20.dp,
    val badgeCornerRadius: Dp = 8.dp,
    val chipCornerRadius: Dp = 20.dp,
    val touchTargetMin: Dp = 48.dp,
    val buttonMinHeight: Dp = 48.dp
)

val LocalCustomerSpacing = staticCompositionLocalOf { CustomerSpacing() }
