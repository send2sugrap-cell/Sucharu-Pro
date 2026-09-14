package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Animated pulse skeleton line for loading states.
 */
@Composable
fun SkeletonLine(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null
) {
    val transition = rememberInfiniteTransition(label = "CustomerSkeletonTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CustomerSkeletonAlpha"
    )

    val baseModifier = if (width != null) modifier.width(width) else modifier.fillMaxWidth()

    Box(
        modifier = baseModifier
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(CustomerTheme.colors.elevatedSurface.copy(alpha = alpha))
    )
}

/**
 * Reusable Card Skeleton loader for Customer/Affiliate feeds.
 */
@Composable
fun CardSkeletonLoader(
    modifier: Modifier = Modifier
) {
    SucharuWallCard(modifier = modifier) {
        Column {
            SkeletonLine(height = 16.dp, width = 120.dp)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.md))
            SkeletonLine(height = 24.dp, width = 220.dp)
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.sm))
            SkeletonLine(height = 12.dp, width = 160.dp)
        }
    }
}
