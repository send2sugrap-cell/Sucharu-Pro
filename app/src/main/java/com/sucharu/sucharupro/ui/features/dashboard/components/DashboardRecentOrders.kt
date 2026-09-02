package com.sucharu.sucharupro.ui.features.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardJobSummary
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.OrderStatusBadge
import com.sucharu.sucharupro.ui.components.PaymentStatusBadge
import com.sucharu.sucharupro.ui.components.ProductionStageBadge
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.BadgeShape
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Recent Orders List for Sucharu Pro Dashboard.
 *
 * Displays the latest production tickets and client orders.
 *
 * CRITICAL ARCHITECTURAL REQUIREMENT:
 * This component explicitly distinguishes between:
 *  1. Commercial Order Status ([com.sucharu.sucharupro.domain.model.order.OrderStatusType])
 *  2. Production Pipeline Stage ([com.sucharu.sucharupro.domain.model.production.ProductionStageType])
 *
 * Both are displayed side-by-side as separate badges.
 */
@Composable
fun DashboardRecentOrders(
    orders: List<DashboardJobSummary>,
    onOrderClick: (String) -> Unit,
    onViewAllOrdersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Recent Orders & Jobs",
            subtitle = "Latest production tickets and commercial orders",
            actionText = "View All",
            onActionClick = onViewAllOrdersClick
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        if (orders.isEmpty()) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(MaterialTheme.spacing.large)
            ) {
                Text(
                    text = "No recent orders found. Tap '+ New Order' to record your first job.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                orders.forEach { order ->
                    RecentOrderCard(
                        order = order,
                        onClick = { onOrderClick(order.orderId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentOrderCard(
    order: DashboardJobSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColors = MaterialTheme.statusColors

    val cardBorder = when {
        order.isDelayed  -> androidx.compose.foundation.BorderStroke(1.dp, statusColors.orderCancelled.border)
        order.isPriority -> androidx.compose.foundation.BorderStroke(1.dp, statusColors.orderOnHold.border)
        else             -> null
    }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        border = cardBorder,
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        // Row 1: Order ID + Priority/Delayed Flags + Due Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = order.orderId,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (order.isPriority) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(statusColors.orderOnHold.container)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = "Priority",
                                tint = statusColors.orderOnHold.content,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Rush",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.orderOnHold.content,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (order.isDelayed) {
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                    Box(
                        modifier = Modifier
                            .clip(BadgeShape)
                            .background(statusColors.orderCancelled.container)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Delayed",
                                tint = statusColors.orderCancelled.content,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Delayed",
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColors.orderCancelled.content,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = order.deliveryDueTime,
                style = MaterialTheme.typography.labelSmall,
                color = if (order.isDelayed) statusColors.orderCancelled.content else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (order.isDelayed) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Row 2: Job Title (with maxLines and ellipsis protection)
        Text(
            text = order.jobTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Row 3: Customer Info & Quantity (with maxLines and ellipsis protection)
        Text(
            text = "${order.customerName} (${order.customerPhone}) • Qty: ${order.quantity}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        // Row 4: Status Badges (Commercial Status + Production Stage + Payment) & Financials
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Badges group
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Commercial Order Status Badge
                OrderStatusBadge(status = order.jobStatus)

                // 2. Production Stage Badge (if currently in production)
                val stage = order.currentProductionStage
                if (stage != null) {
                    ProductionStageBadge(stage = stage)
                }

                // 3. Payment Status Badge
                PaymentStatusBadge(status = order.paymentStatus)
            }

            // Financial amounts (Total & Due)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = order.totalAmount.formatted(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!order.dueAmount.isZero()) {
                    Text(
                        text = "Due: ${order.dueAmount.formatted()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
