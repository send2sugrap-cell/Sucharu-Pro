package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Status Chip for operational status tags, indicators, or filters.
 *
 * @param label Text label for the status chip.
 * @param modifier Optional modifier.
 * @param dotColor Color for the status indicator dot.
 * @param containerColor Background container color.
 * @param contentColor Text color.
 * @param onClick Optional click listener for filter chip behavior.
 */
@Composable
fun AdminStatusChip(
    label: String,
    modifier: Modifier = Modifier,
    dotColor: Color = AdminTheme.colors.success,
    containerColor: Color = dotColor.copy(alpha = 0.15f),
    contentColor: Color = AdminTheme.colors.primaryText,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(AdminTheme.spacing.chipCornerRadius),
        color = containerColor,
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = AdminTheme.spacing.md,
                vertical = AdminTheme.spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
            Text(
                text = label,
                style = AdminTheme.typography.caption,
                color = contentColor
            )
        }
    }
}
