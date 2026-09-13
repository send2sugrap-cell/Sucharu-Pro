package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Wallet
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
 * Unified Admin Workspace for Module 20 / 23 Affiliate Governance & Wallet Payouts.
 */
@Composable
fun AdminAffiliateGovernanceScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    AdminModuleWorkspaceContainer(
        title = "Affiliate Governance & Wallet Payouts",
        subtitle = "Referral attribution, commission clearance, and immutable wallet ledger disbursements",
        icon = Icons.Default.Campaign,
        canonicalModule = "Module 20 / 23",
        requiredCapability = "REPORT_VIEW_AFFILIATE",
        principal = principal,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Total Active Affiliates",
                value = "12 Partners",
                trendDeltaPercentage = 18.2,
                subtitle = "Active referral partners",
                icon = Icons.Default.Campaign,
                accentColor = AdminTheme.colors.accentPrimary,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Total Disbursed Payouts",
                value = "৳142,500.00",
                trendDeltaPercentage = 15.0,
                subtitle = "Immutable wallet ledger",
                icon = Icons.Default.Wallet,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        AdminSection(
            title = "Affiliate Referral Network & Wallet Balances",
            subtitle = "Active partner commission balances and payout review queues"
        ) {
            AdminCard {
                Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Apex Partner Network (APEX2026)", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminStatusChip(label = "ACTIVE", dotColor = AdminTheme.colors.success)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Green Tech Creators (GREENTECH)", style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        AdminStatusChip(label = "ACTIVE", dotColor = AdminTheme.colors.success)
                    }
                }
            }
        }
    }
}
