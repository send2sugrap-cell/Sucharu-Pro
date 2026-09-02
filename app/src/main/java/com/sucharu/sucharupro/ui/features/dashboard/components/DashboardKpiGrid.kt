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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.dashboard.DashboardKpis
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.SectionHeader
import com.sucharu.sucharupro.ui.theme.spacing
import com.sucharu.sucharupro.ui.theme.statusColors

/**
 * Executive KPI Grid for Sucharu Pro Dashboard.
 *
 * Organized into structured, responsive tiers with role-aware visibility preparation:
 *  1. Primary Sales & Revenue Tier (Hidden for non-financial staff roles when role is set)
 *  2. Production & Operations Tier (Visible to all operational staff)
 *  3. Receivables & Financial Position Tier (Hidden for non-financial staff roles when role is set)
 *  4. Secondary Business Metrics (Stock SKUs, Replacements, Affiliate)
 */
@Composable
fun DashboardKpiGrid(
    kpis: DashboardKpis,
    modifier: Modifier = Modifier,
    userRole: UserRole? = null
) {
    val statusColors = MaterialTheme.statusColors
    val showFinancials = userRole == null || userRole.hasFinancialAccess

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // =====================================================================
        // 1. Sales & Revenue Overview (Financial Access Required)
        // =====================================================================
        if (showFinancials) {
            SectionHeader(
                title = "Sales & Profit Summary",
                subtitle = "Real-time revenue snapshot"
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isTabletOrDesktop = maxWidth >= 600.dp

                if (isTabletOrDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        KpiMetricTile(
                            title = "Today's Sales",
                            value = kpis.todaySales.formatted(),
                            subtext = "Booked today",
                            icon = Icons.Default.Payments,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Weekly Sales",
                            value = kpis.weeklySales.formatted(),
                            subtext = "Current week",
                            icon = Icons.Default.DateRange,
                            iconTint = statusColors.orderConfirmed.content,
                            iconBackground = statusColors.orderConfirmed.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Monthly Sales",
                            value = kpis.monthlySales.formatted(),
                            subtext = "Current month",
                            icon = Icons.Default.CalendarMonth,
                            iconTint = statusColors.stagePrinting.content,
                            iconBackground = statusColors.stagePrinting.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Estimated Profit",
                            value = kpis.profit.formatted(),
                            subtext = "Gross margin",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            iconTint = statusColors.paymentPaid.content,
                            iconBackground = statusColors.paymentPaid.container,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            KpiMetricTile(
                                title = "Today's Sales",
                                value = kpis.todaySales.formatted(),
                                subtext = "Booked today",
                                icon = Icons.Default.Payments,
                                iconTint = MaterialTheme.colorScheme.primary,
                                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                title = "Weekly Sales",
                                value = kpis.weeklySales.formatted(),
                                subtext = "Current week",
                                icon = Icons.Default.DateRange,
                                iconTint = statusColors.orderConfirmed.content,
                                iconBackground = statusColors.orderConfirmed.container,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            KpiMetricTile(
                                title = "Monthly Sales",
                                value = kpis.monthlySales.formatted(),
                                subtext = "Current month",
                                icon = Icons.Default.CalendarMonth,
                                iconTint = statusColors.stagePrinting.content,
                                iconBackground = statusColors.stagePrinting.container,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                title = "Estimated Profit",
                                value = kpis.profit.formatted(),
                                subtext = "Gross margin",
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                iconTint = statusColors.paymentPaid.content,
                                iconBackground = statusColors.paymentPaid.container,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 2. Production & Operations Volume Tier (Visible to all)
        // =====================================================================
        SectionHeader(
            title = "Job Volume & Status",
            subtitle = "Active vs delivered jobs"
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isTabletOrDesktop = maxWidth >= 600.dp

            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    KpiMetricTile(
                        title = "Today's Orders",
                        value = "${kpis.todayOrdersCount}",
                        subtext = "New tickets",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBackground = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        title = "Active Jobs",
                        value = "${kpis.activeJobsCount}",
                        subtext = "In production",
                        icon = Icons.Default.HourglassTop,
                        iconTint = statusColors.orderInProduction.content,
                        iconBackground = statusColors.orderInProduction.container,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        title = "Ready for Delivery",
                        value = "${kpis.readyJobsCount}",
                        subtext = "Awaiting dispatch",
                        icon = Icons.Default.CheckCircle,
                        iconTint = statusColors.orderReady.content,
                        iconBackground = statusColors.orderReady.container,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricTile(
                        title = "Delivered Jobs",
                        value = "${kpis.deliveredJobsCount}",
                        subtext = "Completed total",
                        icon = Icons.Default.CheckCircle,
                        iconTint = statusColors.orderDelivered.content,
                        iconBackground = statusColors.orderDelivered.container,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        KpiMetricTile(
                            title = "Today's Orders",
                            value = "${kpis.todayOrdersCount}",
                            subtext = "New tickets",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconTint = MaterialTheme.colorScheme.primary,
                            iconBackground = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Active Jobs",
                            value = "${kpis.activeJobsCount}",
                            subtext = "In production",
                            icon = Icons.Default.HourglassTop,
                            iconTint = statusColors.orderInProduction.content,
                            iconBackground = statusColors.orderInProduction.container,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        KpiMetricTile(
                            title = "Ready for Delivery",
                            value = "${kpis.readyJobsCount}",
                            subtext = "Awaiting dispatch",
                            icon = Icons.Default.CheckCircle,
                            iconTint = statusColors.orderReady.content,
                            iconBackground = statusColors.orderReady.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Delivered Jobs",
                            value = "${kpis.deliveredJobsCount}",
                            subtext = "Completed total",
                            icon = Icons.Default.CheckCircle,
                            iconTint = statusColors.orderDelivered.content,
                            iconBackground = statusColors.orderDelivered.container,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // =====================================================================
        // 3. Receivables & Financial Position Tier (Financial Access Required)
        // =====================================================================
        if (showFinancials) {
            SectionHeader(
                title = "Receivables & Payables",
                subtitle = "Outstanding balances & cash flow"
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isTabletOrDesktop = maxWidth >= 600.dp

                if (isTabletOrDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        KpiMetricTile(
                            title = "Customer Due",
                            value = kpis.customerDue.formatted(),
                            subtext = "Total receivables",
                            icon = Icons.Default.PendingActions,
                            iconTint = statusColors.paymentUnpaid.content,
                            iconBackground = statusColors.paymentUnpaid.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Vendor Payable",
                            value = kpis.vendorPayable.formatted(),
                            subtext = "Supplier dues",
                            icon = Icons.Default.Receipt,
                            iconTint = statusColors.paymentPartial.content,
                            iconBackground = statusColors.paymentPartial.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Expenses",
                            value = kpis.expense.formatted(),
                            subtext = "Operating cost",
                            icon = Icons.Default.MoneyOff,
                            iconTint = statusColors.orderCancelled.content,
                            iconBackground = statusColors.orderCancelled.container,
                            modifier = Modifier.weight(1f)
                        )
                        KpiMetricTile(
                            title = "Today Received",
                            value = kpis.amountReceived.formatted(),
                            subtext = "Cash / Bank collected",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = statusColors.paymentPaid.content,
                            iconBackground = statusColors.paymentPaid.container,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            KpiMetricTile(
                                title = "Customer Due",
                                value = kpis.customerDue.formatted(),
                                subtext = "Total receivables",
                                icon = Icons.Default.PendingActions,
                                iconTint = statusColors.paymentUnpaid.content,
                                iconBackground = statusColors.paymentUnpaid.container,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                title = "Vendor Payable",
                                value = kpis.vendorPayable.formatted(),
                                subtext = "Supplier dues",
                                icon = Icons.Default.Receipt,
                                iconTint = statusColors.paymentPartial.content,
                                iconBackground = statusColors.paymentPartial.container,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            KpiMetricTile(
                                title = "Expenses",
                                value = kpis.expense.formatted(),
                                subtext = "Operating cost",
                                icon = Icons.Default.MoneyOff,
                                iconTint = statusColors.orderCancelled.content,
                                iconBackground = statusColors.orderCancelled.container,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricTile(
                                title = "Today Received",
                                value = kpis.amountReceived.formatted(),
                                subtext = "Cash / Bank collected",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconTint = statusColors.paymentPaid.content,
                                iconBackground = statusColors.paymentPaid.container,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // 4. Auxiliary Metrics (Stock SKUs, Replacements, Affiliate)
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            KpiMetricTile(
                title = "Finished Stock",
                value = "${kpis.finishedProductStockItems} SKUs",
                subtext = "Tracked products",
                icon = Icons.Default.Inventory2,
                iconTint = statusColors.stockInStock.content,
                iconBackground = statusColors.stockInStock.container,
                modifier = Modifier.weight(1f)
            )
            KpiMetricTile(
                title = "Replacements",
                value = "${kpis.replacementCount}",
                subtext = "Rework pending",
                icon = Icons.Default.SyncProblem,
                iconTint = statusColors.orderOnHold.content,
                iconBackground = statusColors.orderOnHold.container,
                modifier = Modifier.weight(1f)
            )
            if (showFinancials && !kpis.affiliateCommission.isZero()) {
                KpiMetricTile(
                    title = "Affiliate Payout",
                    value = kpis.affiliateCommission.formatted(),
                    subtext = "Commissions",
                    icon = Icons.Default.MonetizationOn,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun KpiMetricTile(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    modifier: Modifier = Modifier
) {
    AppCard(
        modifier = modifier,
        contentPadding = PaddingValues(MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
