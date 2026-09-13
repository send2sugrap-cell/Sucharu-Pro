package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Animated pulse skeleton block component for loading states.
 *
 * @param height Height of the skeleton line/block.
 * @param modifier Optional modifier.
 * @param width Optional fixed width or fillMaxWidth.
 * @param cornerRadius Corner radius for skeleton box.
 */
@Composable
fun AdminSkeletonLine(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp
) {
    val transition = rememberInfiniteTransition(label = "SkeletonTransition")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SkeletonAlpha"
    )

    val baseModifier = if (width != null) modifier.width(width) else modifier.fillMaxWidth()

    Box(
        modifier = baseModifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(AdminTheme.colors.elevatedSurface.copy(alpha = alpha))
    )
}

/**
 * Reusable Card Skeleton loader for Admin Panel loading states.
 */
@Composable
fun AdminCardSkeleton(
    modifier: Modifier = Modifier,
    cardHeight: Dp = 120.dp
) {
    AdminCard(modifier = modifier) {
        Column {
            AdminSkeletonLine(height = 16.dp, width = 120.dp)
            Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
            AdminSkeletonLine(height = 28.dp, width = 200.dp)
            Spacer(modifier = Modifier.height(AdminTheme.spacing.sm))
            AdminSkeletonLine(height = 12.dp, width = 160.dp)
        }
    }
}

/**
 * Reusable KPI Card Grid Skeleton loader.
 */
@Composable
fun AdminKpiGridSkeleton(
    modifier: Modifier = Modifier,
    count: Int = 4
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(count.coerceAtMost(2)) {
                AdminCardSkeleton(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = AdminTheme.spacing.sm)
                )
            }
        }
    }
}
