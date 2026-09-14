package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.components.StatusChip
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.WallActivityItem

/**
 * Personal Activity Item Card for Home Feed.
 */
@Composable
fun HomeActivityCard(
    item: WallActivityItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.History
) {
    SucharuWallCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border,
        contentPadding = CustomerTheme.spacing.md
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CustomerTheme.colors.accentPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(CustomerTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = CustomerTheme.typography.title.copy(fontWeight = FontWeight.SemiBold),
                    color = CustomerTheme.colors.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.timestamp,
                    style = CustomerTheme.typography.caption,
                    color = CustomerTheme.colors.mutedText
                )
            }
            Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
            StatusChip(
                label = item.statusLabel,
                dotColor = CustomerTheme.colors.success
            )
        }
    }
}
