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
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Reusable KPI Metric Card for Sucharu Pro Admin Dashboard and Analytics screens.
 *
 * @param title Metric title label.
 * @param value Formatted KPI number string (e.g. "৳1,450,000.00" or "98.4%").
 * @param modifier Optional modifier.
 * @param subtitle Optional secondary subtitle text.
 * @param trendDeltaPercentage Optional trend percentage delta (e.g. +12.5 or -3.2).
 * @param icon Optional leading metric icon.
 * @param accentColor Accent color for icon container and highlights.
 * @param onClick Optional click listener.
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
                style = AdminTheme.typography.caption,
                color = AdminTheme.colors.secondaryText,
                modifier = Modifier.weight(1f)
            )
            if (icon != null) {
                AdminIconContainer(
                    icon = icon,
                    iconTint = accentColor,
                    containerColor = accentColor.copy(alpha = 0.15f),
                    borderColor = accentColor.copy(alpha = 0.3f),
                    boxSize = 36.dp,
                    iconSize = 18.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.sm))

        Text(
            text = value,
            style = AdminTheme.typography.kpiNumber,
            color = AdminTheme.colors.primaryText
        )

        if (subtitle != null || trendDeltaPercentage != null) {
            Spacer(modifier = Modifier.height(AdminTheme.spacing.xs))
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
                        style = AdminTheme.typography.caption,
                        color = trendColor
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.width(AdminTheme.spacing.xs))
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.mutedText
                    )
                }
            }
        }
    }
}
