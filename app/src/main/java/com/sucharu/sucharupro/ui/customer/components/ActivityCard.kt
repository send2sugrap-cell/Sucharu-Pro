package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Personal Activity Timeline Item Card displaying timestamp, icon, title, and status dot.
 */
@Composable
fun ActivityCard(
    title: String,
    timestamp: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    statusLabel: String = "COMPLETED"
) {
    SucharuWallCard(
        modifier = modifier
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
                Text(text = title, style = CustomerTheme.typography.title, color = CustomerTheme.colors.primaryText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = timestamp, style = CustomerTheme.typography.caption, color = CustomerTheme.colors.mutedText)
            }
            Spacer(modifier = Modifier.width(CustomerTheme.spacing.sm))
            StatusChip(label = statusLabel, dotColor = CustomerTheme.colors.success)
        }
    }
}
