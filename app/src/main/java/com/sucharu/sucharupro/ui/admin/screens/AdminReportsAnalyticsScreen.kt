package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
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
 * Unified Admin Workspace for Module 24 Reports, Analytics & Audit Subsystem.
 */
@Composable
fun AdminReportsAnalyticsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Executive Analytics & Audit Reports",
        subtitle = "15 canonical report categories, 138 report definitions, and multi-format export orchestration",
        icon = Icons.Default.Analytics,
        canonicalModule = "Module 24",
        requiredCapability = "REPORT_VIEW_EXECUTIVE_ANALYTICS",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Report Categories",
                value = "15 Categories",
                subtitle = "100% ERP Coverage",
                icon = Icons.Default.Analytics,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Total Report Definitions",
                value = "138 Definitions",
                subtitle = "CSV, JSON, PDF, EXCEL Export",
                icon = Icons.Default.Analytics,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "15 Canonical Report Categories",
            subtitle = "Sales, Customer, Order, Production, Quality, Inventory, Delivery, Finance, Profitability, Affiliate, Wallet, Machine, Preflight, Audit, Executive"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("1. Sales & Revenue Executive Summary", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        Text("Module 03", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("2. Production & QC Stage Performance", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        Text("Module 04 / 06", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3. Machine OEE & Telemetry Health", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        Text("Module 21", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                }
            }
        }
    }
}
