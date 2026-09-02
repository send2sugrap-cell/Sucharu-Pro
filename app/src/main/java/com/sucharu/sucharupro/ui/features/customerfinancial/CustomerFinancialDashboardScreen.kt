package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.CustomerFinancialActionDto
import com.sucharu.sucharupro.data.api.model.CustomerFinancialActivityItemDto
import com.sucharu.sucharupro.data.api.model.CustomerFinancialDashboardDto
import com.sucharu.sucharupro.data.api.model.CustomerFinancialWarningDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.math.BigDecimal

/**
 * Customer Financial Dashboard, Receivable Intelligence & Action Center Screen (Module 14 Step 09).
 */
@Composable
fun CustomerFinancialDashboardScreen(
    dashboard: CustomerFinancialDashboardDto?,
    onActionClick: (CustomerFinancialActionDto) -> Unit = {},
    onViewStatementClick: () -> Unit = {},
    onRecordPaymentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (dashboard == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No financial data available for this customer.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // 1. Header & Financial Overview
        item {
            CustomerHeaderCard(dashboard)
        }

        // 2. Core Financial KPI Grid
        item {
            FinancialKpiSection(dashboard)
        }

        // 3. Action Center & Urgent Recommendations
        if (dashboard.recommendedActions.isNotEmpty()) {
            item {
                Text(
                    text = "Action Center & Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dashboard.recommendedActions) { action ->
                ActionCenterCard(action = action, onClick = { onActionClick(action) })
            }
        }

        // 4. Critical Warnings & Risk Alerts
        if (dashboard.warnings.isNotEmpty()) {
            item {
                Text(
                    text = "Financial Warnings & Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dashboard.warnings) { warning ->
                WarningAlertCard(warning = warning)
            }
        }

        // 5. Receivable Aging Buckets
        item {
            ReceivableAgingCard(dashboard)
        }

        // 6. Due Schedule Breakdown
        item {
            DueScheduleCard(dashboard)
        }

        // 7. Recent Financial Activity
        if (dashboard.recentActivity.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Financial Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(dashboard.recentActivity) { activity ->
                ActivityItemCard(activity = activity)
            }
        }
    }
}

@Composable
private fun CustomerHeaderCard(dashboard: CustomerFinancialDashboardDto) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = dashboard.customerDisplayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Code: ${dashboard.customerCode} • A/C: ${dashboard.accountNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val statusColor = when (dashboard.accountStatus) {
                    "ACTIVE" -> Color(0xFF10B981)
                    "SUSPENDED" -> Color(0xFFEF4444)
                    else -> Color(0xFF6B7280)
                }
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dashboard.accountStatus,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (dashboard.financialHold) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(MaterialTheme.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hold",
                        tint = Color(0xFFEF4444)
                    )
                    Text(
                        text = "FINANCIAL HOLD: ${dashboard.holdReason ?: "Manual Hold"}",
                        color = Color(0xFFEF4444),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialKpiSection(dashboard: CustomerFinancialDashboardDto) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            KpiMiniCard(
                modifier = Modifier.weight(1f),
                title = "Total Outstanding",
                amount = dashboard.outstandingReceivable,
                accentColor = if (dashboard.outstandingReceivable > BigDecimal.ZERO) Color(0xFFF59E0B) else Color(0xFF10B981)
            )
            KpiMiniCard(
                modifier = Modifier.weight(1f),
                title = "Credit Limit",
                amount = dashboard.creditLimit,
                accentColor = Color(0xFF3B82F6)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            KpiMiniCard(
                modifier = Modifier.weight(1f),
                title = "Available Credit",
                amount = dashboard.availableCreditCapacity,
                accentColor = if (dashboard.availableCreditCapacity < BigDecimal.ZERO) Color(0xFFEF4444) else Color(0xFF10B981)
            )
            KpiMiniCard(
                modifier = Modifier.weight(1f),
                title = "Unallocated Payments",
                amount = dashboard.totalUnallocated,
                accentColor = Color(0xFF8B5CF6)
            )
        }
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    amount: BigDecimal,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "৳ $amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ActionCenterCard(
    action: CustomerFinancialActionDto,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (action.actionType) {
                    "REVIEW_HOLD" -> Icons.Default.Warning
                    "REVIEW_COLLECTION" -> Icons.Default.Assignment
                    "ALLOCATE_PAYMENT" -> Icons.Default.Payment
                    "REVIEW_CREDIT" -> Icons.Default.TrendingUp
                    "REVIEW_RECONCILIATION" -> Icons.Default.CheckCircle
                    else -> Icons.Default.Receipt
                }
                val iconColor = when (action.priority) {
                    "CRITICAL" -> Color(0xFFEF4444)
                    "HIGH" -> Color(0xFFF59E0B)
                    else -> Color(0xFF3B82F6)
                }
                Icon(imageVector = icon, contentDescription = action.title, tint = iconColor)
                Column {
                    Text(text = action.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(text = action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Button(onClick = onClick) {
                Text(text = "Action")
            }
        }
    }
}

@Composable
private fun WarningAlertCard(warning: CustomerFinancialWarningDto) {
    val tint = when (warning.severity) {
        "CRITICAL" -> Color(0xFFEF4444)
        "HIGH" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = warning.title, tint = tint)
            Column {
                Text(text = warning.title, fontWeight = FontWeight.Bold, color = tint, style = MaterialTheme.typography.bodyMedium)
                Text(text = warning.message, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReceivableAgingCard(dashboard: CustomerFinancialDashboardDto) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Receivable Aging", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val aging = dashboard.agingSummary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AgingColumn("Current", aging.currentAmount)
                AgingColumn("1–7d", aging.days1To7Amount)
                AgingColumn("8–30d", aging.days8To30Amount)
                AgingColumn("31–60d", aging.days31To60Amount)
                AgingColumn("61–90d", aging.days61To90Amount)
                AgingColumn("90d+", aging.days90PlusAmount, isCritical = true)
            }
        }
    }
}

@Composable
private fun AgingColumn(label: String, amount: BigDecimal, isCritical: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "৳ $amount",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCritical && amount > BigDecimal.ZERO) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DueScheduleCard(dashboard: CustomerFinancialDashboardDto) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Payment Due Schedule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            val due = dashboard.dueSchedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Upcoming Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳ ${due.upcomingDueAmount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "Due Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳ ${due.dueTodayAmount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                }
                Column {
                    Text(text = "Overdue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳ ${due.overdueAmount} (${due.overdueInvoiceCount})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun ActivityItemCard(activity: CustomerFinancialActivityItemDto) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = activity.type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = activity.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            }
            if (activity.amount != null) {
                Text(text = "৳ ${activity.amount}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
