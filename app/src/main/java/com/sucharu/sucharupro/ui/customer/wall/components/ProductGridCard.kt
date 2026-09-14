package com.sucharu.sucharupro.ui.customer.wall.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sucharu.sucharupro.ui.customer.components.StatusChip
import com.sucharu.sucharupro.ui.customer.components.SucharuWallCard
import com.sucharu.sucharupro.ui.customer.theme.CustomerTheme
import com.sucharu.sucharupro.ui.customer.wall.WallProductItem

/**
 * Product Grid Card for Product Catalogue items.
 */
@Composable
fun ProductGridCard(
    item: WallProductItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
            StatusChip(
                label = item.category,
                dotColor = CustomerTheme.colors.accentPurple
            )

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))

            Text(
                text = item.title,
                style = CustomerTheme.typography.title.copy(fontWeight = FontWeight.SemiBold),
                color = CustomerTheme.colors.primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(CustomerTheme.spacing.xs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.priceFormatted,
                    style = CustomerTheme.typography.bodyBold,
                    color = CustomerTheme.colors.success
                )

                Text(
                    text = "Order ->",
                    style = CustomerTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                    color = CustomerTheme.colors.accentPrimary
                )
            }
        }
    }
}
