package com.sucharu.sucharupro.ui.features.customer.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Foundation overview card presenting customer relationship metrics (Orders, Invoices, Challans, Payments).
 *
 * NOTE: This is a future-ready presentation foundation only. Underlying business modules will connect
 * their respective counts in future modules without requiring architectural redesign.
 */
@Composable
fun CustomerRelationshipOverview(
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(MaterialTheme.spacing.large)
    ) {
        SectionHeader(
            title = "Relationship Overview",
            subtitle = "Connected business records & transactions"
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    RelationshipMetricTile(
                        icon = Icons.Default.ShoppingBag,
                        label = "Orders",
                        count = "0",
                        subtitle = "Active / Completed",
                        modifier = Modifier.weight(1f)
                    )
                    RelationshipMetricTile(
                        icon = Icons.Default.Description,
                        label = "Invoices",
                        count = "0",
                        subtitle = "Billed accounts",
                        modifier = Modifier.weight(1f)
                    )
                    RelationshipMetricTile(
                        icon = Icons.Default.LocalShipping,
                        label = "Challans",
                        count = "0",
                        subtitle = "Deliveries",
                        modifier = Modifier.weight(1f)
                    )
                    RelationshipMetricTile(
                        icon = Icons.Default.Payments,
                        label = "Payments",
                        count = "0",
                        subtitle = "Settled receipts",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        RelationshipMetricTile(
                            icon = Icons.Default.ShoppingBag,
                            label = "Orders",
                            count = "0",
                            subtitle = "Active / Completed",
                            modifier = Modifier.weight(1f)
                        )
                        RelationshipMetricTile(
                            icon = Icons.Default.Description,
                            label = "Invoices",
                            count = "0",
                            subtitle = "Billed accounts",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        RelationshipMetricTile(
                            icon = Icons.Default.LocalShipping,
                            label = "Challans",
                            count = "0",
                            subtitle = "Deliveries",
                            modifier = Modifier.weight(1f)
                        )
                        RelationshipMetricTile(
                            icon = Icons.Default.Payments,
                            label = "Payments",
                            count = "0",
                            subtitle = "Settled receipts",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipMetricTile(
    icon: ImageVector,
    label: String,
    count: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(MaterialTheme.spacing.medium)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
