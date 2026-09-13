package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.dashboard.DashboardSummary
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
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
import com.sucharu.sucharupro.ui.features.dashboard.DashboardUiState
import com.sucharu.sucharupro.ui.features.dashboard.DashboardViewModel
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Primary Unified ERP Command Center Dashboard Screen for Sucharu Pro Admin Panel.
 *
 * Orchestrates real-time canonical data across Modules 00–24 into actionable widgets:
 * 1. Executive KPI Grid (Orders, Production, QC, Inventory, Receivables, Collections)
 * 2. 13-Stage Production Pipeline Widget
 * 3. Recent Orders & Commercial Pipeline Widget
 * 4. Financial Settlement & Payments Summary Widget
 * 5. Finished Inventory & Substrate Stock Alerts Widget
 * 6. Quick Actions Command Bar (Capability Guarded)
 */
@Composable
fun UnifiedAdminDashboardScreen(
    viewModel: DashboardViewModel,
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (destination: AppDestination) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val isDesktop = screenWidthDp >= 840

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                AdminDashboardHeader(
                    principal = principal,
                    isLoading = true,
                    onRefresh = { viewModel.refresh() }
                )
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                AdminKpiGridSkeleton(count = 4)
                Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                AdminCardSkeleton(cardHeight = 220.dp)
            }

            is DashboardUiState.Error -> {
                AdminDashboardHeader(
                    principal = principal,
                    isLoading = false,
                    onRefresh = { viewModel.refresh() }
                )
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                AdminCard(
                    accentBarColor = AdminTheme.colors.error,
                    contentPadding = AdminTheme.spacing.xl
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AdminIconContainer(
                            icon = Icons.Default.Warning,
                            iconTint = AdminTheme.colors.error,
                            containerColor = AdminTheme.colors.errorContainer,
                            borderColor = AdminTheme.colors.error
                        )
                        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                        Text(
                            text = "Dashboard Service Unavailable",
                            style = AdminTheme.typography.cardTitle,
                            color = AdminTheme.colors.primaryText
                        )
                        Spacer(modifier = Modifier.height(AdminTheme.spacing.xs))
                        Text(
                            text = state.errorMessage,
                            style = AdminTheme.typography.body,
                            color = AdminTheme.colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                        AdminButton(
                            text = "Retry Dashboard Data Load",
                            onClick = { viewModel.retry() },
                            style = AdminButtonStyle.SECONDARY,
                            icon = Icons.Default.Refresh
                        )
                    }
                }
            }

            is DashboardUiState.Empty -> {
                AdminDashboardHeader(
                    principal = principal,
                    isLoading = false,
                    onRefresh = { viewModel.refresh() }
                )
                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))
                AdminEmptyState(
                    message = "No Active Operational Data Found",
                    subtitle = "Initialize commercial orders or production job cards to populate real-time command widgets.",
                    actionLabel = "New Order",
                    onActionClick = { onNavigateToDestination(AppDestination.Customer.Orders) }
                )
            }

            is DashboardUiState.Success -> {
                val summary = state.summary

                // 1. Dashboard Header Section
                AdminDashboardHeader(
                    principal = principal,
                    isLoading = state.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )

                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                // 2. Executive KPI Grid
                AdminSection(
                    title = "Executive ERP Performance",
                    subtitle = "Real-time canonical metrics across active ERP modules"
                ) {
                    AdminExecutiveKpiGrid(
                        summary = summary,
                        onNavigateToDestination = onNavigateToDestination
                    )
                }

                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                // 3. Quick Action Command Bar
                AdminSection(
                    title = "Capability-Guarded Quick Actions",
                    subtitle = "Direct operational shortcuts to canonical ERP workspaces"
                ) {
                    AdminQuickActionsBar(
                        onNavigateToDestination = onNavigateToDestination
                    )
                }

                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                // 4. Production Pipeline & 13 Canonical Stages Widget
                if (isDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            AdminProductionPipelineWidget(
                                summary = summary,
                                onNavigateToDestination = onNavigateToDestination
                            )
                        }
                        Column(modifier = Modifier.weight(0.8f)) {
                            AdminFinancialSummaryWidget(
                                summary = summary,
                                onNavigateToDestination = onNavigateToDestination
                            )
                        }
                    }
                } else {
                    AdminProductionPipelineWidget(
                        summary = summary,
                        onNavigateToDestination = onNavigateToDestination
                    )
                    Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                    AdminFinancialSummaryWidget(
                        summary = summary,
                        onNavigateToDestination = onNavigateToDestination
                    )
                }

                Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

                // 5. Recent Orders & Finished Goods Inventory Alerts
                if (isDesktop) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AdminRecentOrdersWidget(
                                summary = summary,
                                onNavigateToDestination = onNavigateToDestination
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            AdminInventoryAlertsWidget(
                                summary = summary,
                                onNavigateToDestination = onNavigateToDestination
                            )
                        }
                    }
                } else {
                    AdminRecentOrdersWidget(
                        summary = summary,
                        onNavigateToDestination = onNavigateToDestination
                    )
                    Spacer(modifier = Modifier.height(AdminTheme.spacing.md))
                    AdminInventoryAlertsWidget(
                        summary = summary,
                        onNavigateToDestination = onNavigateToDestination
                    )
                }

                Spacer(modifier = Modifier.height(AdminTheme.spacing.xxl))
            }
        }
    }
}

@Composable
private fun AdminDashboardHeader(
    principal: AuthenticatedPrincipal?,
    isLoading: Boolean,
    onRefresh: () -> Unit
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
                containerColor = AdminTheme.colors.accentContainer,
                boxSize = 48.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Good day, ${principal?.username ?: "Admin"}",
                        style = AdminTheme.typography.pageTitle,
                        color = AdminTheme.colors.primaryText
                    )
                    Spacer(modifier = Modifier.width(AdminTheme.spacing.sm))
                    AdminBadge(
                        text = "LIVE",
                        containerColor = AdminTheme.colors.successContainer,
                        contentColor = AdminTheme.colors.success,
                        borderColor = AdminTheme.colors.success
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Project: ${principal?.projectId ?: "PRJ-001"} • System Operational Status: Healthy",
                    style = AdminTheme.typography.caption,
                    color = AdminTheme.colors.secondaryText
                )
            }
        }

        AdminButton(
            text = if (isLoading) "Refreshing..." else "Refresh",
            onClick = onRefresh,
            style = AdminButtonStyle.OUTLINED,
            icon = Icons.Default.Refresh,
            isLoading = isLoading
        )
    }
}

@Composable
private fun AdminExecutiveKpiGrid(
    summary: DashboardSummary,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    val kpis = summary.kpis

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Today's Commercial Orders",
                value = "${kpis.todayOrdersCount} Orders",
                subtitle = "Active in pipeline",
                icon = Icons.Default.ShoppingCart,
                accentColor = AdminTheme.colors.accentPrimary,
                onClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Active Production Jobs",
                value = "${kpis.activeJobsCount} Running",
                subtitle = "13 Canonical Stages",
                icon = Icons.Default.Engineering,
                accentColor = AdminTheme.colors.accentPurple,
                onClick = { onNavigateToDestination(AppDestination.Staff.Production) },
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Ready for Dispatch",
                value = "${kpis.readyJobsCount} Ready",
                subtitle = "Completed & Inspected",
                icon = Icons.Default.CheckCircle,
                accentColor = AdminTheme.colors.info,
                onClick = { onNavigateToDestination(AppDestination.Staff.Delivery) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Customer Accounts Receivable",
                value = kpis.customerDue.formatted("BDT "),
                subtitle = "Invoice Due Total",
                icon = Icons.Default.AccountBalance,
                accentColor = AdminTheme.colors.warning,
                onClick = { onNavigateToDestination(AppDestination.Admin.Finance) },
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Today's Collections",
                value = kpis.amountReceived.formatted("BDT "),
                subtitle = "Bank & Cash Receipts",
                icon = Icons.Default.MonetizationOn,
                accentColor = AdminTheme.colors.success,
                onClick = { onNavigateToDestination(AppDestination.Admin.Finance) },
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Finished Goods Stock SKUs",
                value = "${kpis.finishedProductStockItems} SKUs",
                subtitle = "Warehouse Stock Total",
                icon = Icons.Default.PrecisionManufacturing,
                accentColor = AdminTheme.colors.accentPrimary,
                onClick = { onNavigateToDestination(AppDestination.Manager.Inventory) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AdminQuickActionsBar(
    onNavigateToDestination: (AppDestination) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)
    ) {
        AdminButton(
            text = "New Order",
            onClick = { onNavigateToDestination(AppDestination.Customer.Quotations) },
            style = AdminButtonStyle.PRIMARY,
            icon = Icons.Default.Add,
            modifier = Modifier.weight(1f)
        )
        AdminButton(
            text = "Job Cards",
            onClick = { onNavigateToDestination(AppDestination.Staff.Production) },
            style = AdminButtonStyle.SECONDARY,
            icon = Icons.Default.Engineering,
            modifier = Modifier.weight(1f)
        )
        AdminButton(
            text = "Challans",
            onClick = { onNavigateToDestination(AppDestination.Staff.Delivery) },
            style = AdminButtonStyle.SECONDARY,
            icon = Icons.Default.LocalShipping,
            modifier = Modifier.weight(1f)
        )
        AdminButton(
            text = "Finance GL",
            onClick = { onNavigateToDestination(AppDestination.Admin.Finance) },
            style = AdminButtonStyle.OUTLINED,
            icon = Icons.Default.AccountBalance,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdminProductionPipelineWidget(
    summary: DashboardSummary,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    AdminCard(
        accentBarColor = AdminTheme.colors.accentPurple
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminIconContainer(
                    icon = Icons.Default.Engineering,
                    iconTint = AdminTheme.colors.accentPurple,
                    containerColor = AdminTheme.colors.accentPurple.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
                Column {
                    Text(
                        text = "13 Canonical Stages Pipeline",
                        style = AdminTheme.typography.cardTitle,
                        color = AdminTheme.colors.primaryText
                    )
                    Text(
                        text = "Shop-floor active job execution density",
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.secondaryText
                    )
                }
            }

            AdminButton(
                text = "View All",
                onClick = { onNavigateToDestination(AppDestination.Staff.Production) },
                style = AdminButtonStyle.TEXT
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

        val stages = ProductionStageType.orderedStages
        val totalJobsInPipeline = summary.stageCounts.sumOf { it.count }.coerceAtLeast(1)

        stages.take(6).forEach { stage ->
            val count = summary.stageCounts.find { it.stage == stage }?.count ?: 0
            val fraction = (count.toFloat() / totalJobsInPipeline.toFloat()).coerceIn(0f, 1f)

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stage.defaultLabel,
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.primaryText
                    )
                    Text(
                        text = "$count Jobs",
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.secondaryText
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = AdminTheme.colors.accentPurple,
                    trackColor = AdminTheme.colors.elevatedSurface
                )
            }
        }
    }
}

@Composable
private fun AdminFinancialSummaryWidget(
    summary: DashboardSummary,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    val breakdown = summary.paymentBreakdown

    AdminCard(
        accentBarColor = AdminTheme.colors.success
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AdminIconContainer(
                    icon = Icons.Default.MonetizationOn,
                    iconTint = AdminTheme.colors.success,
                    containerColor = AdminTheme.colors.successContainer
                )
                Spacer(modifier = Modifier.width(AdminTheme.spacing.md))
                Column {
                    Text(
                        text = "3-Way Finance Settlement",
                        style = AdminTheme.typography.cardTitle,
                        color = AdminTheme.colors.primaryText
                    )
                    Text(
                        text = "Invoices vs Collections vs Due",
                        style = AdminTheme.typography.caption,
                        color = AdminTheme.colors.secondaryText
                    )
                }
            }

            AdminButton(
                text = "Ledger",
                onClick = { onNavigateToDestination(AppDestination.Admin.Finance) },
                style = AdminButtonStyle.TEXT
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(AdminTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Invoiced Today", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                Text(breakdown.totalInvoicedToday.formatted("BDT "), style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Collected Today", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                Text(breakdown.totalCollectedToday.formatted("BDT "), style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.success)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Outstanding Due", style = AdminTheme.typography.body, color = AdminTheme.colors.secondaryText)
                Text(breakdown.totalOutstandingDue.formatted("BDT "), style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.warning)
            }
        }
    }
}

@Composable
private fun AdminRecentOrdersWidget(
    summary: DashboardSummary,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    AdminCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Commercial Orders", style = AdminTheme.typography.cardTitle, color = AdminTheme.colors.primaryText)
            AdminButton(
                text = "Orders",
                onClick = { onNavigateToDestination(AppDestination.Customer.Orders) },
                style = AdminButtonStyle.TEXT
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

        if (summary.recentOrders.isEmpty()) {
            AdminEmptyState(message = "No Recent Orders Found")
        } else {
            summary.recentOrders.take(4).forEach { job ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AdminTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(job.jobTitle, style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        Text(job.customerName, style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                    AdminStatusChip(label = job.jobStatus.defaultLabel, dotColor = AdminTheme.colors.accentPrimary)
                }
            }
        }
    }
}

@Composable
private fun AdminInventoryAlertsWidget(
    summary: DashboardSummary,
    onNavigateToDestination: (AppDestination) -> Unit
) {
    AdminCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Finished Goods Low Stock Alerts", style = AdminTheme.typography.cardTitle, color = AdminTheme.colors.primaryText)
            AdminButton(
                text = "Inventory",
                onClick = { onNavigateToDestination(AppDestination.Manager.Inventory) },
                style = AdminButtonStyle.TEXT
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.md))

        if (summary.inventoryAlerts.isEmpty()) {
            AdminEmptyState(message = "All Stock Levels Healthy")
        } else {
            summary.inventoryAlerts.take(4).forEach { alert ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AdminTheme.spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(alert.itemName, style = AdminTheme.typography.bodyBold, color = AdminTheme.colors.primaryText)
                        Text(alert.category, style = AdminTheme.typography.caption, color = AdminTheme.colors.secondaryText)
                    }
                    AdminBadge(
                        text = "${alert.currentStock.toInt()} / ${alert.minThreshold.toInt()} ${alert.unit}",
                        containerColor = AdminTheme.colors.warningContainer,
                        contentColor = AdminTheme.colors.warning,
                        borderColor = AdminTheme.colors.warning
                    )
                }
            }
        }
    }
}
