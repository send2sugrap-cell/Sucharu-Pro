package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.components.AdminStatusChip
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 21 Machine Telemetry & OEE.
 */
@Composable
fun AdminMachineOeeScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Machine Telemetry & OEE Command",
        subtitle = "Equipment asset registry, sensor telemetry streams, downtime, and OEE metrics",
        icon = Icons.Default.PrecisionManufacturing,
        canonicalModule = "Module 21",
        requiredCapability = "REPORT_VIEW_MACHINE_OPERATIONS",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Overall Plant OEE",
                value = "84.2%",
                trendDeltaPercentage = 2.4,
                subtitle = "Target >= 80%",
                icon = Icons.Default.PrecisionManufacturing,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Availability Score",
                value = "91.5%",
                trendDeltaPercentage = 1.0,
                subtitle = "26,352 Run Time Sec",
                icon = Icons.Default.PrecisionManufacturing,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "Registered Equipment Assets & Telemetry Streams",
            subtitle = "Heidelberg Speedmaster 4-Color Offset Press & Digital Press assets"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("MAC-PRESS-001 • Heidelberg Speedmaster XL 106", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminStatusChip(label = "IN_USE", dotColor = AdminTheme.colors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Speed: 4,850 SPH • Temperature: 42.5°C", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                }
            }
        }
    }
}
