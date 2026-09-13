package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable Admin Badge pill tag for status, categories, or counts.
 *
 * @param text Badge text label.
 * @param modifier Optional modifier.
 * @param containerColor Background fill color.
 * @param contentColor Text color.
 * @param borderColor Optional border stroke color.
 */
@Composable
fun AdminBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = AdminTheme.colors.elevatedSurface,
    contentColor: Color = AdminTheme.colors.primaryText,
    borderColor: Color? = AdminTheme.colors.border
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AdminTheme.spacing.badgeCornerRadius),
        color = containerColor,
        border = if (borderColor != null) BorderStroke(1.dp, borderColor) else null
    ) {
        Text(
            text = text,
            style = AdminTheme.typography.caption,
            color = contentColor,
            modifier = Modifier.padding(
                horizontal = AdminTheme.spacing.sm,
                vertical = AdminTheme.spacing.xs
            )
        )
    }
}
