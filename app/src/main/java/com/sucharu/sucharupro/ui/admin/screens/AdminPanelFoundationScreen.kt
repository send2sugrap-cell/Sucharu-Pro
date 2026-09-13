package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.ui.admin.components.AdminBadge
import com.sucharu.sucharupro.ui.admin.components.AdminButton
import com.sucharu.sucharupro.ui.admin.components.AdminButtonStyle
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminCardSkeleton
import com.sucharu.sucharupro.ui.admin.components.AdminEmptyState
import com.sucharu.sucharupro.ui.admin.components.AdminIconContainer
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiGridSkeleton
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.components.AdminStatusChip
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme

/**
 * Sucharu Pro Admin Panel Foundation Screen.
 *
 * Demonstrates and verifies the complete Admin Design System foundation:
 * - AdminTheme (AdminColors, AdminTypography, AdminSpacing)
 * - Reusable UI Components (AdminCard, AdminSection, AdminButton, AdminKpiCard, AdminBadge, AdminStatusChip, AdminEmptyState, AdminSkeleton)
 * - Dark-mode-first aesthetic with responsive grid spacing
 */
@Composable
fun AdminPanelFoundationScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    var isLoading by remember { mutableStateOf(false) }
    var showEmptyState by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("ALL") }

    AdminTheme {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = AdminTheme.colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = AdminTheme.spacing.screenPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                // 1. Top Header Area
                AdminHeaderSection(
                    isLoading = isLoading,
                    onRefreshToggle = { isLoading = !isLoading },
                    onEmptyToggle = { showEmptyState = !showEmptyState }
                )

                Spacer(modifier = Modifier.height(AdminTheme.spacing.xl))

                if (isLoading) {
                    // 2. Loading State Verification
                    AdminSection(
                        title = "System Analytics (Loading Skeleton)",
                        subtitle = "Verifying animated shimmer pulse skeleton state"
                    ) {
                        AdminKpiGridSkeleton(count = 2)
                        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                        AdminCardSkeleton()
                    }
                } else if (showEmptyState) {
                    // 3. Empty State Verification
                    AdminSection(
                        title = "Filtered Admin Records",
                        subtitle = "Verifying empty state presentation with action handler"
                    ) {
                        AdminEmptyState(
                            message = "No Admin Records Match Selected Criteria",
                            subtitle = "Try adjusting active filters or refreshing the administrative dashboard.",
                            actionLabel = "Reset Filters",
                            onActionClick = { showEmptyState = false }
                        )
                    }
                } else {
                    // 4. Primary KPI Metrics Grid
                    AdminSection(
                        title = "Executive ERP Performance",
                        subtitle = "Real-time canonical metrics across active ERP modules"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
                        ) {
                            AdminKpiCard(
                                title = "Gross Invoiced Revenue",
                                value = "৳2,450,000.00",
                                trendDeltaPercentage = 14.2,
                                subtitle = "vs last month",
                                icon = Icons.Default.Analytics,
                                accentColor = AdminTheme.colors.accentPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "Plant OEE Score",
                                value = "84.2%",
                                trendDeltaPercentage = 2.4,
                                subtitle = "Target >= 80%",
                                icon = Icons.Default.PrecisionManufacturing,
                                accentColor = AdminTheme.colors.success,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
                        ) {
                            AdminKpiCard(
                                title = "Active Commercial Orders",
                                value = "1,248 Orders",
                                trendDeltaPercentage = 8.5,
                                subtitle = "100% on schedule",
                                icon = Icons.Default.ShoppingCart,
                                accentColor = AdminTheme.colors.accentPurple,
                                modifier = Modifier.weight(1f)
                            )
                            AdminKpiCard(
                                title = "Preflight Pass Rate",
                                value = "98.4%",
                                trendDeltaPercentage = 1.2,
                                subtitle = "0 blocking findings",
                                icon = Icons.Default.CheckCircle,
                                accentColor = AdminTheme.colors.info,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                    // 5. Status Filter Chips & Badges Gallery
                    AdminSection(
                        title = "Operational Status & Filter Controls",
                        subtitle = "Interactive filter chips with semantic indicator dots"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)
                        ) {
                            AdminStatusChip(
                                label = "ALL SYSTEMS",
                                dotColor = AdminTheme.colors.info,
                                onClick = { activeFilter = "ALL" }
                            )
                            AdminStatusChip(
                                label = "OPERATIONAL",
                                dotColor = AdminTheme.colors.success,
                                onClick = { activeFilter = "OPERATIONAL" }
                            )
                            AdminStatusChip(
                                label = "MAINTENANCE",
                                dotColor = AdminTheme.colors.warning,
                                onClick = { activeFilter = "MAINTENANCE" }
                            )
                            AdminStatusChip(
                                label = "CRITICAL",
                                dotColor = AdminTheme.colors.error,
                                onClick = { activeFilter = "CRITICAL" }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                    // 6. Component Preview Card with Accent Bar
                    AdminSection(
                        title = "Security & Governance Overview",
                        subtitle = "Role Capability Matrix & PostgreSQL RLS Enforcement"
                    ) {
                        AdminCard(
                            accentBarColor = AdminTheme.colors.accentPrimary
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AdminIconContainer(
                                        icon = Icons.Default.Shield,
                                        iconTint = AdminTheme.colors.accentPrimary,
                                        containerColor = AdminTheme.colors.accentContainer
                                    )
                                    Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
                                    Column {
                                        Text(
                                            text = "Multi-Tenant Isolation & RBAC Guard",
                                            style = AdminTheme.typography.cardTitle,
                                            color = AdminTheme.colors.primaryText
                                        )
                                        Text(
                                            text = "Capabilities: REPORT_VIEW, EXPORT, GOVERNANCE_WRITE",
                                            style = AdminTheme.typography.caption,
                                            color = AdminTheme.colors.secondaryText
                                        )
                                    }
                                }
                                AdminBadge(
                                    text = "ENFORCED",
                                    containerColor = AdminTheme.colors.successContainer,
                                    contentColor = AdminTheme.colors.success,
                                    borderColor = AdminTheme.colors.success
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                    // 7. Button Styles Gallery
                    AdminSection(
                        title = "Interactive Admin Action Buttons",
                        subtitle = "Accessibility touch target size >= 48dp with loading state support"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)
                        ) {
                            AdminButton(
                                text = "Primary Action",
                                onClick = { },
                                style = AdminButtonStyle.PRIMARY,
                                modifier = Modifier.weight(1f)
                            )
                            AdminButton(
                                text = "Secondary",
                                onClick = { },
                                style = AdminButtonStyle.SECONDARY,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(AdminTheme.spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)
                        ) {
                            AdminButton(
                                text = "Outlined Action",
                                onClick = { },
                                style = AdminButtonStyle.OUTLINED,
                                modifier = Modifier.weight(1f)
                            )
                            AdminButton(
                                text = "Text Action",
                                onClick = { },
                                style = AdminButtonStyle.TEXT,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AdminTheme.spacing.xxl))
            }
        }
    }
}

@Composable
private fun AdminHeaderSection(
    isLoading: Boolean,
    onRefreshToggle: () -> Unit,
    onEmptyToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AdminIconContainer(
                icon = Icons.Default.AdminPanelSettings,
                iconTint = AdminTheme.colors.accentPrimary,
                containerColor = AdminTheme.colors.accentContainer,
                boxSize = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Admin Panel Foundation",
                        style = AdminTheme.typography.pageTitle,
                        color = AdminTheme.colors.primaryText
                    )
                    Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
                    AdminBadge(
                        text = "PHASE 01",
                        containerColor = AdminTheme.colors.accentContainer,
                        contentColor = AdminTheme.colors.accentPrimary,
                        borderColor = AdminTheme.colors.accentPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Dark-Mode-First Enterprise UI/UX Design System",
                    style = AdminTheme.typography.caption,
                    color = AdminTheme.colors.secondaryText
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.xs)) {
            AdminButton(
                text = if (isLoading) "Stop Loading" else "Simulate Load",
                onClick = onRefreshToggle,
                style = AdminButtonStyle.OUTLINED,
                icon = Icons.Default.Refresh
            )
        }
    }
}
