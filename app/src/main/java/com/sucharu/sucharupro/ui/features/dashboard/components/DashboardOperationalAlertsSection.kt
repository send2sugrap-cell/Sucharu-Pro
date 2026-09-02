package com.sucharu.sucharupro.ui.features.dashboard.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardOperationalAlerts
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.BadgeShape
import com.sucharu.sucharupro.ui.theme.StatusColor
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

private data class AlertItemData(
    val title: String,
    val count: Int,
    val subtitle: String,
    val icon: ImageVector,
    val statusColor: StatusColor,
    val onClick: () -> Unit
)

/**
 * Operational Alerts Section for Sucharu Pro Dashboard.
 *
 * Highlights real-time operational bottlenecks and items requiring manager/staff attention.
 * Tapping any alert routes to the corresponding destination.
 * Prepared for role-aware filtering based on [userRole].
 */
@Composable
fun DashboardOperationalAlertsSection(
    alerts: DashboardOperationalAlerts,
    onPendingApprovalClick: () -> Unit = {},
    onQcPendingClick: () -> Unit = {},
    onDeliveryPendingClick: () -> Unit = {},
    onDelayedJobsClick: () -> Unit = {},
    onLowStockClick: () -> Unit = {},
    onUnpaidInvoicesClick: () -> Unit = {},
    onVendorDueClick: () -> Unit = {},
    onReplacementClick: () -> Unit = {},
    onViewAllAlertsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    if (!alerts.hasAnyAlert) return

    val statusColors = MaterialTheme.statusColors

    val showDelayed = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val showDelivery = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.WAREHOUSE)
    val showApproval = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.DESIGNER, UserRole.STAFF)
    val showQc = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR)
    val showStock = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)
    val showFinancialAlerts = userRole == null || userRole.hasFinancialAccess
    val showReplacement = userRole == null || userRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.QC_INSPECTOR, UserRole.STAFF)

    val activeAlerts = buildList {
        if (showDelayed && alerts.delayedJobsCount > 0) {
            add(
                AlertItemData(
                    title = "Delayed Jobs",
                    count = alerts.delayedJobsCount,
                    subtitle = "Past deadline",
                    icon = Icons.Default.Warning,
                    statusColor = statusColors.orderCancelled,
                    onClick = onDelayedJobsClick
                )
            )
        }
        if (showDelivery && alerts.deliveryPendingCount > 0) {
            add(
                AlertItemData(
                    title = "Delivery Today",
                    count = alerts.deliveryPendingCount,
                    subtitle = "Ready to dispatch",
                    icon = Icons.Default.LocalShipping,
                    statusColor = statusColors.orderReady,
                    onClick = onDeliveryPendingClick
                )
            )
        }
        if (showApproval && alerts.pendingApprovalCount > 0) {
            add(
                AlertItemData(
                    title = "Approval Needed",
                    count = alerts.pendingApprovalCount,
                    subtitle = "Customer proofs",
                    icon = Icons.Default.HourglassBottom,
                    statusColor = statusColors.orderPending,
                    onClick = onPendingApprovalClick
                )
            )
        }
        if (showQc && alerts.qcPendingCount > 0) {
            add(
                AlertItemData(
                    title = "QC Pending",
                    count = alerts.qcPendingCount,
                    subtitle = "Inspection required",
                    icon = Icons.AutoMirrored.Filled.FactCheck,
                    statusColor = statusColors.stageQc,
                    onClick = onQcPendingClick
                )
            )
        }
        if (showStock && alerts.lowStockAlertCount > 0) {
            add(
                AlertItemData(
                    title = "Low Stock SKUs",
                    count = alerts.lowStockAlertCount,
                    subtitle = "Finished products",
                    icon = Icons.Default.Inventory2,
                    statusColor = statusColors.stockLow,
                    onClick = onLowStockClick
                )
            )
        }
        if (showFinancialAlerts && alerts.outstandingPaymentCount > 0) {
            add(
                AlertItemData(
                    title = "Unpaid Invoices",
                    count = alerts.outstandingPaymentCount,
                    subtitle = "Overdue balances",
                    icon = Icons.Default.MoneyOff,
                    statusColor = statusColors.paymentUnpaid,
                    onClick = onUnpaidInvoicesClick
                )
            )
        }
        if (showFinancialAlerts && alerts.vendorDueCount > 0) {
            add(
                AlertItemData(
                    title = "Vendor Due",
                    count = alerts.vendorDueCount,
                    subtitle = "Supplier payables",
                    icon = Icons.Default.Receipt,
                    statusColor = statusColors.paymentPartial,
                    onClick = onVendorDueClick
                )
            )
        }
        if (showReplacement && alerts.replacementPendingCount > 0) {
            add(
                AlertItemData(
                    title = "Replacement",
                    count = alerts.replacementPendingCount,
                    subtitle = "Rework cycle",
                    icon = Icons.Default.SyncProblem,
                    statusColor = statusColors.orderOnHold,
                    onClick = onReplacementClick
                )
            )
        }
    }

    if (activeAlerts.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Operational Alerts",
            subtitle = "${activeAlerts.sumOf { it.count }} actionable items requiring attention",
            actionText = "Review All",
            onActionClick = onViewAllAlertsClick
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp
            val columns = if (isTabletOrDesktop) 4 else 2
            val chunkedAlerts = activeAlerts.chunked(columns)

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                chunkedAlerts.forEach { rowAlerts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        rowAlerts.forEach { item ->
                            OperationalAlertCard(
                                title = item.title,
                                count = item.count,
                                subtitle = item.subtitle,
                                icon = item.icon,
                                statusColor = item.statusColor,
                                onClick = item.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val emptySlots = columns - rowAlerts.size
                        repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OperationalAlertCard(
    title: String,
    count: Int,
    subtitle: String,
    icon: ImageVector,
    statusColor: StatusColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.border.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(statusColor.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor.content,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(BadgeShape)
                    .background(statusColor.container)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor.content,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
