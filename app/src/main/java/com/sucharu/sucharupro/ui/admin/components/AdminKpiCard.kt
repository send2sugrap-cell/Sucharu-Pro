package com.sucharu.sucharupro.ui.admin.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable KPI Metric Card for Sucharu Pro Admin Dashboard and Analytics screens with compact typography.
 */
@Composable
fun AdminKpiCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trendDeltaPercentage: Double? = null,
    icon: ImageVector? = null,
    accentColor: Color = AdminTheme.colors.accentPrimary,
    onClick: (() -> Unit)? = null
) {
    AdminCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = AdminTheme.colors.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = AdminTheme.typography.caption.copy(lineHeight = 14.sp),
                color = AdminTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (icon != null) {
                AdminIconContainer(
                    icon = icon,
                    iconTint = accentColor,
                    containerColor = accentColor.copy(alpha = 0.15f),
                    borderColor = accentColor.copy(alpha = 0.3f),
                    boxSize = 32.dp,
                    iconSize = 16.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = AdminTheme.typography.kpiNumber.copy(lineHeight = 28.sp),
            color = AdminTheme.colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (subtitle != null || trendDeltaPercentage != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (trendDeltaPercentage != null) {
                    val isPositive = trendDeltaPercentage >= 0
                    val trendColor = if (isPositive) AdminTheme.colors.success else AdminTheme.colors.error
                    val trendIcon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

                    Icon(
                        imageVector = trendIcon,
                        contentDescription = if (isPositive) "Positive Trend" else "Negative Trend",
                        tint = trendColor,
                        modifier = Modifier.width(12.dp).height(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(java.util.Locale.US, "%.1f%%", trendDeltaPercentage)}",
                        style = AdminTheme.typography.caption.copy(lineHeight = 12.sp),
                        color = trendColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.width(AdminTheme.spacing.xs))
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AdminTheme.typography.caption.copy(lineHeight = 12.sp),
                        color = AdminTheme.colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
