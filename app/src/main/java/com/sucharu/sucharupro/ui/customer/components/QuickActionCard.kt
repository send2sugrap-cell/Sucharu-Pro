package com.sucharu.sucharupro.ui.customer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme

/**
 * Mobile Touch-Friendly Quick Action Card with minimum touch target size >= 48dp.
 */
@Composable
fun QuickActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SucharuWallCard(
        modifier = modifier
            .defaultMinSize(minHeight = CustomerTheme.spacing.touchTargetMin)
            .fillMaxWidth(),
        onClick = onClick,
        containerColor = CustomerTheme.colors.elevatedSurface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CustomerTheme.colors.accentPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))
            Text(
                text = label,
                style = CustomerTheme.typography.caption,
                color = CustomerTheme.colors.primaryText,
                textAlign = TextAlign.Center
            )
        }
    }
}
