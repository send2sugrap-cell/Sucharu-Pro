package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.components.AdminStatusChip
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 05 / 22 Preflight Engine & Proof Validation.
 */
@Composable
fun AdminPreflightDiagnosticsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Preflight Diagnostics & Proofing",
        subtitle = "Automated preflight inspection, rule execution, diagnostic findings, and readiness gates",
        icon = Icons.Default.CheckCircle,
        canonicalModule = "Module 05 / 22",
        requiredCapability = "REPORT_VIEW_PREFLIGHT",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Preflight Pass Rate",
                value = "100.0%",
                trendDeltaPercentage = 1.2,
                subtitle = "0 blocking findings",
                icon = Icons.Default.CheckCircle,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Total Preflight Runs",
                value = "284 Runs",
                trendDeltaPercentage = 8.0,
                subtitle = "Engine Version v1.0",
                icon = Icons.Default.CheckCircle,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "Recent Preflight Inspection Runs",
            subtitle = "Diagnostic findings, rule executions, and production readiness decisions"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RUN-EX-701 • Artwork ART-101", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminStatusChip(label = "PASS", dotColor = AdminTheme.colors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("RUN-EX-702 • Artwork ART-105", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminBadge(text = "1 WARNING (WAIVED)", containerColor = AdminTheme.colors.warningContainer, contentColor = AdminTheme.colors.warning)
                    }
                }
            }
        }
    }
}
