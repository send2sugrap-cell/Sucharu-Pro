package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 09 / 14 / 15 Finance & General Ledger Operations.
 */
@Composable
fun AdminFinanceOperationsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Financial Operations & General Ledger",
        subtitle = "3-Way Invoicing vs Payments vs Ledger settlement & P&L statements",
        icon = Icons.Default.AccountBalance,
        canonicalModule = "Module 09 / 14 / 15",
        requiredCapability = "REPORT_VIEW_FINANCE",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Total Gross Revenue",
                value = "৳2,450,000.00",
                trendDeltaPercentage = 14.0,
                subtitle = "YTD Sales Invoiced",
                icon = Icons.Default.MonetizationOn,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Outstanding Receivables",
                value = "৳1,850,000.00",
                trendDeltaPercentage = -2.5,
                subtitle = "0.00 BDT Reconciliation Variance",
                icon = Icons.Default.AccountBalance,
                accentColor = AdminTheme.colors.warning,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "3-Way Settlement Reconciliation Status",
            subtitle = "Strict Money / BigDecimal precision matching customer invoices and ledger entries"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Gross Invoiced Subtotal", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳2,450,000.00", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payments Collected", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳600,000.00", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Customer Outstanding Due", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                        Text("৳1,850,000.00", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.warning)
                    }
                }
            }
        }
    }
}
