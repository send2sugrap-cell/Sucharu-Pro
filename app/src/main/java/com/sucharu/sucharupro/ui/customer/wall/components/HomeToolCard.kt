package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Tool & Action Card for AI Assistant, Cost Estimator, Quotation, and Tracking.
 */
@Composable
fun HomeToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = CustomerTheme.colors.accentPrimary
) {
    SucharuWallCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CustomerTheme.spacing.touchTargetMin),
        onClick = onClick,
        containerColor = CustomerTheme.colors.surface,
        borderColor = accentColor.copy(alpha = 0.4f),
        contentPadding = CustomerTheme.spacing.md
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))

            Text(
                text = title,
                style = CustomerTheme.typography.title.copy(fontWeight = FontWeight.Bold),
                color = CustomerTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                style = CustomerTheme.typography.caption,
                color = CustomerTheme.colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
