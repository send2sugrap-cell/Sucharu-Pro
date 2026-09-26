package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.service.finance.FinancialControlCenterService
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * BI-01 Financial Control Center & Collection Intelligence Workspace.
 */
@Composable
fun AdminFinanceOperationsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    val service = remember { FinancialControlCenterService() }
    val summary = remember { service.calculateFinancialControlCenterSummary() }

    AdminModuleWorkspaceContainer(
        title = "BI-01 Financial Control Center & Collection Intelligence",
        subtitle = "3-Way Invoicing vs Payments vs Ledger settlement & Collection Action Center",
        icon = Icons.Default.AccountBalance,
        canonicalModule = "Module 09 / 14 / 15",
        requiredCapability = "REPORT_VIEW_FINANCE",
        principal = principal,
        modifier = modifier
    ) {
        // KPI Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Total Outstanding Receivables",
                value = "৳${summary.totalOutstanding.toInt()}",
                trendDeltaPercentage = -2.5,
                subtitle = "Current Due: ৳${summary.currentDue.toInt()}",
                icon = Icons.Default.AccountBalance,
                accentColor = AdminTheme.colors.warning,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Total Overdue Amount",
                value = "৳${summary.overdueAmount.toInt()}",
                trendDeltaPercentage = 8.5,
                subtitle = "Due Today: ৳${summary.dueTodayAmount.toInt()}",
                icon = Icons.Default.Warning,
                accentColor = AdminTheme.colors.error,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "YTD Payments Collected",
                value = "৳${summary.totalCollectedYtd.toInt()}",
                trendDeltaPercentage = 14.0,
                subtitle = "Unallocated: ৳${summary.unallocatedPaymentsTotal.toInt()}",
                icon = Icons.Default.MonetizationOn,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        // Collection Intelligence & Attention Center
        AdminSection(
            title = "Collection Intelligence & Attention Center",
            subtitle = "Actionable collection list for overdue accounts requiring follow-up"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.attentionItems.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = when (item.attentionCategory.name) {
                                            "LONG_OVERDUE" -> Color(0xFFEF4444)
                                            "OVERDUE" -> Color(0xFFF59E0B)
                                            else -> Color(0xFF10B981)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(text = item.attentionCategory.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                    Text(text = item.customerCode, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = item.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(text = "Phone: ${item.primaryPhone} • Overdue: ${item.overdueDays} days", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "৳${item.totalOutstanding.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF59E0B))
                                Text(text = "${item.outstandingInvoiceCount} Invoices Due", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        // Receivable Aging Buckets
        AdminSection(
            title = "Receivable Aging Distribution (0–90+ Days)",
            subtitle = "Chronological aging breakdown of customer balances"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Due (0–30 Days)", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳${summary.agingSummary.currentDue.toInt()}", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overdue 31–60 Days", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳${summary.agingSummary.overdue31To60Days.toInt()}", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.warning)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overdue 61–90 Days", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳${summary.agingSummary.overdue61To90Days.toInt()}", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.warning)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overdue 90+ Days (Critical)", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳${summary.agingSummary.overdue90PlusDays.toInt()}", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.error)
                    }
                }
            }
        }
    }
}
