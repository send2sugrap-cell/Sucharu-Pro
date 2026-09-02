package com.sucharu.sucharupro.ui.features.governance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.data.api.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.businessfinancialgovernance.*
import com.sucharu.sucharupro.domain.model.businessintegrity.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class GovernanceTab(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    BUDGETS("Budgets", Icons.Default.AccountBalance),
    VARIANCE("Budget vs Actual", Icons.Default.CompareArrows),
    FORECAST("Projections & Scenarios", Icons.Default.TrendingUp),
    ALERTS("Decision Alerts", Icons.Default.NotificationsActive),
    AUDIT("Revisions & Audit", Icons.Default.HistoryEdu),
    INTEGRITY("Final Integrity & Close", Icons.Default.VerifiedUser)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFinancialGovernanceScreen(
    overview: ExecutiveGovernanceOverviewDto? = null,
    budgets: List<BusinessFinancialBudgetDto> = emptyList(),
    comparisons: List<BudgetVsActualComparisonDto> = emptyList(),
    forecasts: List<BusinessFinancialForecastDto> = emptyList(),
    thresholds: List<BusinessFinancialBudgetThresholdDto> = emptyList(),
    alerts: List<BusinessFinancialGovernanceAlertDto> = emptyList(),
    revisions: List<BusinessFinancialBudgetRevisionDto> = emptyList(),
    auditEvents: List<BusinessFinancialGovernanceAuditEventDto> = emptyList(),
    integrityRuns: List<FinancialIntegrityRunDto> = emptyList(),
    periodReadiness: PeriodFinalizationReadinessDto? = null,
    periodCertificates: List<PeriodCloseCertificateDto> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onCreateBudget: (CreateFinancialBudgetRequestDto) -> Unit = {},
    onSubmitBudget: (String) -> Unit = {},
    onReviewBudget: (String) -> Unit = {},
    onApproveBudget: (String) -> Unit = {},
    onActivateBudget: (String) -> Unit = {},
    onRejectBudget: (String, String) -> Unit = { _, _ -> },
    onReviseBudget: (String, BigDecimal, String) -> Unit = { _, _, _ -> },
    onCloseBudget: (String) -> Unit = {},
    onGenerateForecast: (GenerateForecastRequestDto) -> Unit = {},
    onEvaluateAlerts: () -> Unit = {},
    onAcknowledgeAlert: (String, String?) -> Unit = { _, _ -> },
    onResolveAlert: (String, String?) -> Unit = { _, _ -> },
    onDismissAlert: (String, String) -> Unit = { _, _ -> },
    onExecuteIntegrityRun: (String) -> Unit = {},
    onFinalizePeriodClose: (String, String) -> Unit = { _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(GovernanceTab.OVERVIEW) }
    var selectedBudgetId by remember { mutableStateOf<String?>(null) }
    var showCreateBudgetDialog by remember { mutableStateOf(false) }
    var showGenerateForecastDialog by remember { mutableStateOf(false) }

    val bgDark = Color(0xFF0A0F1D)
    val cardBg = Color(0xFF121B2E)
    val cardBorder = Color(0xFF1E2D4A)
    val accentCyan = Color(0xFF00E5FF)
    val accentGreen = Color(0xFF00E676)
    val accentRed = Color(0xFFFF5252)
    val accentAmber = Color(0xFFFFD600)
    val accentPurple = Color(0xFFB388FF)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF90A4AE)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = "Governance Icon",
                            tint = accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Business Financial Governance & Budget Control",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "Controlled Financial Planning, Variance, Forecast & Decision Intelligence",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = accentCyan)
                    }
                    IconButton(onClick = { onEvaluateAlerts() }) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = "Evaluate Rules", tint = accentAmber)
                    }
                    Button(
                        onClick = { showCreateBudgetDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Budget", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Budget", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cardBg)
            )
        },
        containerColor = bgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GovernanceTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accentCyan else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) accentCyan else textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) textPrimary else textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = accentCyan,
                    trackColor = cardBg
                )
            }

            if (errorMessage != null) {
                Surface(
                    color = accentRed.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = accentRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = errorMessage, color = accentRed, fontSize = 13.sp)
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    GovernanceTab.OVERVIEW -> GovernanceOverviewView(
                        overview = overview,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentAmber = accentAmber,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.BUDGETS -> BudgetsListView(
                        budgets = budgets,
                        selectedBudgetId = selectedBudgetId,
                        onSelectBudget = { selectedBudgetId = it },
                        onSubmitBudget = onSubmitBudget,
                        onReviewBudget = onReviewBudget,
                        onApproveBudget = onApproveBudget,
                        onActivateBudget = onActivateBudget,
                        onRejectBudget = onRejectBudget,
                        onReviseBudget = onReviseBudget,
                        onCloseBudget = onCloseBudget,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentAmber = accentAmber,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.VARIANCE -> VarianceComparisonView(
                        comparisons = comparisons,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentAmber = accentAmber,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.FORECAST -> ForecastProjectionsView(
                        forecasts = forecasts,
                        onGenerateForecastClick = { showGenerateForecastDialog = true },
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentPurple = accentPurple,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.ALERTS -> GovernanceAlertsView(
                        alerts = alerts,
                        onAcknowledgeAlert = onAcknowledgeAlert,
                        onResolveAlert = onResolveAlert,
                        onDismissAlert = onDismissAlert,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentAmber = accentAmber,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.AUDIT -> RevisionsAndAuditView(
                        revisions = revisions,
                        auditEvents = auditEvents,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentAmber = accentAmber,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    GovernanceTab.INTEGRITY -> FinalIntegrityControlView(
                        integrityRuns = integrityRuns,
                        periodReadiness = periodReadiness,
                        periodCertificates = periodCertificates,
                        onExecuteIntegrityRun = onExecuteIntegrityRun,
                        onFinalizePeriodClose = onFinalizePeriodClose,
                        accentCyan = accentCyan,
                        accentGreen = accentGreen,
                        accentRed = accentRed,
                        accentAmber = accentAmber,
                        accentPurple = accentPurple,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

// =============================================================================
// SUB-VIEWS
// =============================================================================

@Composable
private fun GovernanceOverviewView(
    overview: ExecutiveGovernanceOverviewDto?,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    if (overview == null) {
        EmptyStateCard("No Executive Governance Overview Data Available", cardBg, textSecondary)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            // Metrics Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GovernanceStatCard(
                    title = "Allocated Budget",
                    value = formatMoney(overview.totalAllocatedBudgetAmount, overview.currency),
                    subtitle = "${overview.totalActiveBudgetsCount} Active Budgets",
                    color = accentCyan,
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary
                )
                GovernanceStatCard(
                    title = "Actual Spend",
                    value = formatMoney(overview.totalActualSpendAmount, overview.currency),
                    subtitle = "Canonical Ledger Actuals",
                    color = accentAmber,
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary
                )
                GovernanceStatCard(
                    title = "Total Projected Exposure",
                    value = formatMoney(overview.totalProjectedExposureAmount, overview.currency),
                    subtitle = "Actuals + Commitments + Accruals",
                    color = if (overview.totalProjectedExposureAmount > overview.totalAllocatedBudgetAmount) accentRed else accentGreen,
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary
                )
                GovernanceStatCard(
                    title = "Remaining Budget",
                    value = formatMoney(overview.totalRemainingBudgetAmount, overview.currency),
                    subtitle = "${overview.overallUtilizationPercentage.toPlainString()}% Utilization",
                    color = if (overview.totalRemainingBudgetAmount < BigDecimal.ZERO) accentRed else accentGreen,
                    modifier = Modifier.weight(1f),
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textSecondary = textSecondary
                )
            }
        }

        item {
            // Exposure & Alerts summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exposure breakdown card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Exposure Composition",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryRow("Committed Unfulfilled", formatMoney(overview.totalCommittedExposureAmount, overview.currency), accentCyan)
                        SummaryRow("Active Accruals", formatMoney(overview.totalAccruedExposureAmount, overview.currency), accentAmber)
                        SummaryRow("Active Thresholds", "${overview.activeThresholdsCount} Configured", textSecondary)
                    }
                }

                // Alert summary card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Decision Alert Status",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SummaryRow("Open Alerts", "${overview.openAlertsCount}", if (overview.openAlertsCount > 0) accentAmber else accentGreen)
                        SummaryRow("Critical Alerts", "${overview.criticalAlertsCount}", if (overview.criticalAlertsCount > 0) accentRed else textSecondary)
                        SummaryRow("Warning Alerts", "${overview.warningAlertsCount}", if (overview.warningAlertsCount > 0) accentAmber else textSecondary)
                    }
                }
            }
        }

        item {
            Text(
                text = "Budget Utilization Breakdown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(overview.comparisons) { comp ->
            VarianceCard(
                comp = comp,
                accentCyan = accentCyan,
                accentGreen = accentGreen,
                accentRed = accentRed,
                accentAmber = accentAmber,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun BudgetsListView(
    budgets: List<BusinessFinancialBudgetDto>,
    selectedBudgetId: String?,
    onSelectBudget: (String) -> Unit,
    onSubmitBudget: (String) -> Unit,
    onReviewBudget: (String) -> Unit,
    onApproveBudget: (String) -> Unit,
    onActivateBudget: (String) -> Unit,
    onRejectBudget: (String, String) -> Unit,
    onReviseBudget: (String, BigDecimal, String) -> Unit,
    onCloseBudget: (String) -> Unit,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    if (budgets.isEmpty()) {
        EmptyStateCard("No Financial Budgets Found", cardBg, textSecondary)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(budgets) { budget ->
            val isExpanded = selectedBudgetId == budget.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isExpanded) accentCyan else cardBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectBudget(budget.id) },
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = budget.budgetName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(budget.status.name, budget.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Dimension: ${budget.dimensionType} • ID: ${budget.dimensionId} • Rev v${budget.version}",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatMoney(budget.allocatedAmount, budget.currency),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentCyan
                            )
                            Text(
                                text = "Period: ${budget.periodId}",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                        }
                    }

                    if (isExpanded) {
                        Divider(color = cardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        if (!budget.description.isNullOrBlank()) {
                            Text(text = "Notes: ${budget.description}", fontSize = 12.sp, color = textSecondary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "Created by: ${budget.createdBy} | Approved by: ${budget.approvedBy ?: "N/A"}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Lifecycle Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (budget.status) {
                                BusinessFinancialBudgetStatus.DRAFT -> {
                                    Button(
                                        onClick = { onSubmitBudget(budget.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                                    ) {
                                        Text("Submit for Review", color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                                BusinessFinancialBudgetStatus.SUBMITTED -> {
                                    Button(
                                        onClick = { onReviewBudget(budget.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentAmber)
                                    ) {
                                        Text("Mark Reviewed", color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                                BusinessFinancialBudgetStatus.REVIEWED -> {
                                    Button(
                                        onClick = { onApproveBudget(budget.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                                    ) {
                                        Text("Approve", color = Color.Black, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onRejectBudget(budget.id, "Rejected by management.") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed)
                                    ) {
                                        Text("Reject", fontSize = 12.sp)
                                    }
                                }
                                BusinessFinancialBudgetStatus.APPROVED -> {
                                    Button(
                                        onClick = { onActivateBudget(budget.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                                    ) {
                                        Text("Activate Budget", color = Color.Black, fontSize = 12.sp)
                                    }
                                }
                                BusinessFinancialBudgetStatus.ACTIVE -> {
                                    Button(
                                        onClick = { onCloseBudget(budget.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF455A64))
                                    ) {
                                        Text("Close Period Budget", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VarianceComparisonView(
    comparisons: List<BudgetVsActualComparisonDto>,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    if (comparisons.isEmpty()) {
        EmptyStateCard("No Variance Comparisons Available", cardBg, textSecondary)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(comparisons) { comp ->
            VarianceCard(
                comp = comp,
                accentCyan = accentCyan,
                accentGreen = accentGreen,
                accentRed = accentRed,
                accentAmber = accentAmber,
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }
    }
}

@Composable
private fun VarianceCard(
    comp: BudgetVsActualComparisonDto,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = comp.budgetName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Dimension: ${comp.dimensionType} (${comp.dimensionId})",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                VarianceStatusBadge(comp.varianceStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Utilization Progress Bar
            val pctFloat = (comp.utilizationPercentage.toFloat() / 100f).coerceIn(0f, 1f)
            val barColor = when (comp.varianceStatus) {
                BudgetVarianceStatus.OVER_BUDGET, BudgetVarianceStatus.CRITICAL -> accentRed
                BudgetVarianceStatus.WARNING -> accentAmber
                else -> accentGreen
            }

            LinearProgressIndicator(
                progress = { pctFloat },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = cardBorder
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Allocated: ${formatMoney(comp.allocatedBudget, comp.currency)}", fontSize = 12.sp, color = textSecondary)
                    Text(text = "Actual Spend: ${formatMoney(comp.actualSpend, comp.currency)}", fontSize = 12.sp, color = textPrimary, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Committed: ${formatMoney(comp.committedExposure, comp.currency)}", fontSize = 12.sp, color = accentCyan)
                    Text(text = "Remaining: ${formatMoney(comp.remainingBudget, comp.currency)}", fontSize = 12.sp, color = if (comp.remainingBudget < BigDecimal.ZERO) accentRed else accentGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ForecastProjectionsView(
    forecasts: List<BusinessFinancialForecastDto>,
    onGenerateForecastClick: () -> Unit,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentPurple: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Deterministic Forecast Projections",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Button(
                onClick = onGenerateForecastClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentPurple)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = "Run Forecast", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Forecast", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (forecasts.isEmpty()) {
            EmptyStateCard("No Financial Forecasts Generated Yet", cardBg, textSecondary)
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(forecasts) { fc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = fc.forecastName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Dimension: ${fc.dimensionType} (${fc.dimensionId}) • Period: ${fc.periodId}",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatMoney(fc.forecastTotalAmount, fc.currency),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCyan
                                )
                                Text(
                                    text = "Run-rate: ${formatMoney(fc.runRatePerDay, fc.currency)} / day",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Divider(color = cardBorder, modifier = Modifier.padding(vertical = 10.dp))

                        Text(
                            text = "Scenario Projections",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        fc.scenarios.forEach { sc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sc.scenarioType.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (sc.scenarioType) {
                                        ForecastScenarioType.OPTIMISTIC -> accentGreen
                                        ForecastScenarioType.CONSERVATIVE -> accentRed
                                        ForecastScenarioType.BASELINE -> accentCyan
                                    }
                                )
                                Text(
                                    text = formatMoney(sc.projectedAmount, fc.currency),
                                    fontSize = 13.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Variance vs Budget: ${formatMoney(sc.varianceVsBudget, fc.currency)}",
                                    fontSize = 11.sp,
                                    color = if (sc.varianceVsBudget < BigDecimal.ZERO) accentRed else accentGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GovernanceAlertsView(
    alerts: List<BusinessFinancialGovernanceAlertDto>,
    onAcknowledgeAlert: (String, String?) -> Unit,
    onResolveAlert: (String, String?) -> Unit,
    onDismissAlert: (String, String) -> Unit,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    if (alerts.isEmpty()) {
        EmptyStateCard("No Governance Alerts Active", cardBg, textSecondary)
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(alerts) { alert ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        when (alert.severity) {
                            GovernanceAlertSeverity.CRITICAL -> accentRed
                            GovernanceAlertSeverity.WARNING -> accentAmber
                            GovernanceAlertSeverity.INFO -> cardBorder
                        },
                        RoundedCornerShape(12.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlertSeverityBadge(alert.severity)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = alert.alertType.name.replace("_", " "),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        AlertStatusBadge(alert.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = alert.message, fontSize = 13.sp, color = textPrimary)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Threshold: ${alert.thresholdValue.toPlainString()} | Current: ${alert.currentValue.toPlainString()} | Dimension: ${alert.sourceDimensionType} (${alert.sourceDimensionId})",
                        fontSize = 11.sp,
                        color = textSecondary
                    )

                    if (alert.status == GovernanceAlertStatus.OPEN) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onAcknowledgeAlert(alert.id, "Acknowledged by manager.") },
                                colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                            ) {
                                Text("Acknowledge", color = Color.Black, fontSize = 11.sp)
                            }
                            Button(
                                onClick = { onResolveAlert(alert.id, "Resolved following budget adjustment.") },
                                colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                            ) {
                                Text("Resolve", color = Color.Black, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onDismissAlert(alert.id, "Dismissed after managerial review.") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentRed)
                            ) {
                                Text("Dismiss", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionsAndAuditView(
    revisions: List<BusinessFinancialBudgetRevisionDto>,
    auditEvents: List<BusinessFinancialGovernanceAuditEventDto>,
    accentCyan: Color,
    accentGreen: Color,
    accentAmber: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(
                text = "Budget Revisions History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
        }

        if (revisions.isEmpty()) {
            item { EmptyStateCard("No Budget Revisions Recorded", cardBg, textSecondary) }
        } else {
            items(revisions) { rev ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Rev v${rev.version} • Budget: ${rev.budgetId}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = "Reason: ${rev.revisionReason}", fontSize = 11.sp, color = textSecondary)
                            Text(text = "Revised by: ${rev.revisedBy}", fontSize = 10.sp, color = textSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "${rev.previousAllocatedAmount} -> ${rev.newAllocatedAmount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentCyan)
                            Text(text = formatDate(rev.revisedAt), fontSize = 10.sp, color = textSecondary)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Governance Audit Trail",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (auditEvents.isEmpty()) {
            item { EmptyStateCard("No Audit Events Recorded", cardBg, textSecondary) }
        } else {
            items(auditEvents) { ev ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "${ev.eventType} • ${ev.outcome}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (ev.outcome == "SUCCESS") accentGreen else accentAmber)
                            Text(text = "Actor: ${ev.actorId} (${ev.actorRole}) • Target: ${ev.targetType} (${ev.targetId})", fontSize = 11.sp, color = textSecondary)
                        }
                        Text(text = formatDate(ev.timestamp), fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}

// =============================================================================
// REUSABLE COMPONENTS & BADGES
// =============================================================================

@Composable
private fun GovernanceStatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    cardBg: Color,
    cardBorder: Color,
    textSecondary: Color
) {
    Card(
        modifier = modifier.border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 12.sp, color = textSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, fontSize = 11.sp, color = textSecondary)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF90A4AE))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun StatusBadge(label: String, status: BusinessFinancialBudgetStatus) {
    val (bg, fg) = when (status) {
        BusinessFinancialBudgetStatus.ACTIVE -> Color(0xFF00E676).copy(alpha = 0.2f) to Color(0xFF00E676)
        BusinessFinancialBudgetStatus.APPROVED -> Color(0xFF00E5FF).copy(alpha = 0.2f) to Color(0xFF00E5FF)
        BusinessFinancialBudgetStatus.REVIEWED, BusinessFinancialBudgetStatus.SUBMITTED -> Color(0xFFFFD600).copy(alpha = 0.2f) to Color(0xFFFFD600)
        BusinessFinancialBudgetStatus.REJECTED -> Color(0xFFFF5252).copy(alpha = 0.2f) to Color(0xFFFF5252)
        BusinessFinancialBudgetStatus.REVISED -> Color(0xFFB388FF).copy(alpha = 0.2f) to Color(0xFFB388FF)
        BusinessFinancialBudgetStatus.CLOSED -> Color(0xFF78909C).copy(alpha = 0.2f) to Color(0xFFB0BEC5)
        BusinessFinancialBudgetStatus.DRAFT -> Color(0xFF455A64).copy(alpha = 0.3f) to Color(0xFFCFD8DC)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(text = label, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun VarianceStatusBadge(status: BudgetVarianceStatus) {
    val (bg, fg) = when (status) {
        BudgetVarianceStatus.ON_TRACK -> Color(0xFF00E676).copy(alpha = 0.2f) to Color(0xFF00E676)
        BudgetVarianceStatus.WARNING -> Color(0xFFFFD600).copy(alpha = 0.2f) to Color(0xFFFFD600)
        BudgetVarianceStatus.OVER_BUDGET -> Color(0xFFFF5252).copy(alpha = 0.2f) to Color(0xFFFF5252)
        BudgetVarianceStatus.CRITICAL -> Color(0xFFFF1744).copy(alpha = 0.2f) to Color(0xFFFF1744)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(text = status.displayName, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun AlertSeverityBadge(severity: GovernanceAlertSeverity) {
    val (bg, fg) = when (severity) {
        GovernanceAlertSeverity.CRITICAL -> Color(0xFFFF5252).copy(alpha = 0.2f) to Color(0xFFFF5252)
        GovernanceAlertSeverity.WARNING -> Color(0xFFFFD600).copy(alpha = 0.2f) to Color(0xFFFFD600)
        GovernanceAlertSeverity.INFO -> Color(0xFF00E5FF).copy(alpha = 0.2f) to Color(0xFF00E5FF)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(text = severity.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun AlertStatusBadge(status: GovernanceAlertStatus) {
    val (bg, fg) = when (status) {
        GovernanceAlertStatus.OPEN -> Color(0xFFFFD600).copy(alpha = 0.2f) to Color(0xFFFFD600)
        GovernanceAlertStatus.ACKNOWLEDGED -> Color(0xFF00E5FF).copy(alpha = 0.2f) to Color(0xFF00E5FF)
        GovernanceAlertStatus.RESOLVED -> Color(0xFF00E676).copy(alpha = 0.2f) to Color(0xFF00E676)
        GovernanceAlertStatus.DISMISSED -> Color(0xFF78909C).copy(alpha = 0.2f) to Color(0xFFB0BEC5)
    }
    Surface(color = bg, shape = RoundedCornerShape(6.dp)) {
        Text(text = status.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
    }
}

@Composable
private fun EmptyStateCard(text: String, cardBg: Color, textSecondary: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, color = textSecondary, fontSize = 14.sp)
        }
    }
}

private fun formatMoney(amount: BigDecimal, currency: String): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    return "$currency ${formatter.format(amount)}"
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(timestamp))
}

@Composable
private fun FinalIntegrityControlView(
    integrityRuns: List<FinancialIntegrityRunDto>,
    periodReadiness: PeriodFinalizationReadinessDto?,
    periodCertificates: List<PeriodCloseCertificateDto>,
    onExecuteIntegrityRun: (String) -> Unit,
    onFinalizePeriodClose: (String, String) -> Unit,
    accentCyan: Color,
    accentGreen: Color,
    accentRed: Color,
    accentAmber: Color,
    accentPurple: Color,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    var periodInput by remember { mutableStateOf("PER-2026-M08") }
    val latestRun = integrityRuns.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Control Header & Action Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Financial Integrity & Period Finalization Control",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "18 Canonical Cross-Module Control Assertions & Cryptographic Period Closure",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = { onExecuteIntegrityRun(periodInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentCyan)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run 18 Assertions", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Period Close Readiness Assessment
        if (periodReadiness != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (periodReadiness.isReadyForClose) accentGreen.copy(alpha = 0.1f) else accentRed.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (periodReadiness.isReadyForClose) accentGreen else accentRed
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (periodReadiness.isReadyForClose) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = "Readiness",
                                    tint = if (periodReadiness.isReadyForClose) accentGreen else accentRed,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Period Closure Readiness: ${periodReadiness.status}",
                                        color = textPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Period: ${periodReadiness.periodCode} | Latest Run: ${periodReadiness.latestRunStatus ?: "N/A"}",
                                        color = textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            if (periodReadiness.isReadyForClose) {
                                Button(
                                    onClick = { onFinalizePeriodClose(periodReadiness.periodId, "Period finalized via Governance UI") },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = "Close", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finalize & Hard-Close", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        if (periodReadiness.blockingReasons.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Blocking Issues (${periodReadiness.blockingReasons.size}):", color = accentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            periodReadiness.blockingReasons.forEach { reason ->
                                Text("• $reason", color = accentRed, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        // 18 Assertions Details from Latest Run
        if (latestRun != null && latestRun.assertions.isNotEmpty()) {
            item {
                Text(
                    text = "Control Assertions (${latestRun.passedAssertionsCount} Passed, ${latestRun.warningAssertionsCount} Warning, ${latestRun.failedAssertionsCount} Failed)",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            items(latestRun.assertions) { assertion ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${assertion.assertionType}: ${assertion.assertionName}",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            val badgeColor = when (assertion.status) {
                                "PASSED" -> accentGreen
                                "WARNING" -> accentAmber
                                else -> accentRed
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = assertion.status,
                                    color = badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = assertion.explanation, color = textSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Actual: ${assertion.actualValue} | Expected: ${assertion.expectedValue}",
                            color = textSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Period Close Certificates
        if (periodCertificates.isNotEmpty()) {
            item {
                Text(
                    text = "Tamper-Evident Period Closure Certificates",
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            items(periodCertificates) { cert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentPurple.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Period ${cert.periodCode} Closure Certificate",
                                color = accentPurple,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Status: ${cert.status}",
                                color = accentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SHA-256 Checksum: ${cert.certificateChecksum}",
                            color = accentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expenses: ${formatMoney(cert.totalRecognizedExpenses, "BDT")} | Settled Payables: ${formatMoney(cert.totalSettledPayables, "BDT")}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Ledger Debit: ${formatMoney(cert.totalLedgerDebit, "BDT")} | Ledger Credit: ${formatMoney(cert.totalLedgerCredit, "BDT")}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
