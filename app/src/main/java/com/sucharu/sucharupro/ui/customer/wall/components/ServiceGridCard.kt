package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.WallServiceItem

/**
 * Service Grid Card for Printing Services.
 */
@Composable
fun ServiceGridCard(
    item: WallServiceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Print
) {
    SucharuWallCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CustomerTheme.spacing.touchTargetMin),
        onClick = onClick,
        containerColor = CustomerTheme.colors.surface,
        borderColor = CustomerTheme.colors.border,
        contentPadding = CustomerTheme.spacing.md
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = item.title,
                tint = CustomerTheme.colors.accentPrimary,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))

            Text(
                text = item.title,
                style = CustomerTheme.typography.title.copy(fontWeight = FontWeight.SemiBold),
                color = CustomerTheme.colors.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = item.description,
                style = CustomerTheme.typography.caption,
                color = CustomerTheme.colors.secondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
