package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for Module 04 Production Execution & 13 Canonical Stages.
 */
@Composable
fun AdminProductionOperationsScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Production Operations Center",
        subtitle = "Shop-floor active job execution density across 13 canonical stages",
        icon = Icons.Default.Engineering,
        canonicalModule = "Module 04",
        requiredCapability = "STAFF_READ_ORDERS",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Active Production Jobs",
                value = "18 Running",
                trendDeltaPercentage = 4.5,
                subtitle = "Shop-floor queue",
                icon = Icons.Default.Engineering,
                accentColor = AdminTheme.colors.accentPurple,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "13 Canonical Stages",
                value = "DESIGN -> DELIVERED",
                subtitle = "Strict order preservation",
                icon = Icons.Default.Engineering,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "13 Canonical Production Pipeline Stages",
            subtitle = "Active work orders progressing through shop-floor stages"
        ) {
            AdminCard {
                ProductionStageType.orderedStages.forEach { stage ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stage.defaultLabel, style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                            Text("Code: ${stage.shortCode}", style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { 0.4f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = AdminTheme.colors.accentPurple,
                            trackColor = AdminTheme.colors.elevatedSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
