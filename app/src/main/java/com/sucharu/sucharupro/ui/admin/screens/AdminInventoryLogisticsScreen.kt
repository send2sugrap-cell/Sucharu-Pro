package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 07 Finished Goods Inventory & Module 08 Delivery.
 */
@Composable
fun AdminInventoryLogisticsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Inventory & Logistics Operations",
        subtitle = "Finished product stock levels, warehouse stock locations, and challan dispatches",
        icon = Icons.Default.Inventory,
        canonicalModule = "Module 07 / 08",
        requiredCapability = "STAFF_READ_INVENTORY",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Finished Goods SKUs",
                value = "540 SKUs",
                subtitle = "Valuation: ৳4,850,000.00",
                icon = Icons.Default.Inventory,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Challans Dispatched",
                value = "12 Dispatches",
                trendDeltaPercentage = 6.0,
                subtitle = "In-transit shipments",
                icon = Icons.Default.LocalShipping,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "Finished Goods Inventory Balances",
            subtitle = "Tracked saleable product inventory (No raw material stock)"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Custom Business Cards 300GSM", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminBadge(text = "12,500 Pcs", containerColor = AdminTheme.colors.accentContainer, contentColor = AdminTheme.colors.accentPrimary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("A4 Corporate Brochure", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminBadge(text = "5,000 Pcs", containerColor = AdminTheme.colors.accentContainer, contentColor = AdminTheme.colors.accentPrimary)
                    }
                }
            }
        }
    }
}
