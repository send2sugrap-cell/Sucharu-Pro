package com.sucharu.sucharupro.ui.features.profitability.executive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.*

/**
 * Production Jetpack Compose Executive Command Center for Profitability & KPI Cockpit.
 * Module 16 Step 10.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutiveProfitabilityCommandCenterScreen(
    snapshot: ExecutiveProfitabilitySnapshotDto?,
    kpis: List<ExecutiveKpiDto>,
    scorecard: ExecutiveManagementScorecardDto?,
    rankings: ExecutiveRankingsPayloadDto?,
    priorities: List<ExecutivePriorityItemDto>,
    drivers: List<ExecutiveProfitabilityDriverDto>,
    leakage: ExecutiveLeakageSummaryDto?,
    concentration: ExecutiveConcentrationSummaryDto?,
    reconciliation: ExecutiveReconciliationResultDto?,
    report: ExecutiveProfitabilityReportDto?,
    onRecalculateClick: () -> Unit = {},
    onExportHandoffClick: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf(
        "Overview",
        "KPI Cockpit",
        "Scorecard",
        "Rankings",
        "Drivers & Leakage",
        "Priorities",
        "Concentration",
        "Reconciliation",
        "Executive Report"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Executive Profitability Cockpit", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Command Center & Management Reporting", style = MaterialTheme.typography.bodySmall)
                    }
                },
                actions = {
                    IconButton(onClick = onRecalculateClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate Snapshot")
                    }
                    IconButton(onClick = onExportHandoffClick) {
                        Icon(Icons.Default.Share, contentDescription = "Export AI Handoff Contract")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ExecutiveOverviewTab(snapshot, scorecard, priorities)
                1 -> ExecutiveKpiCockpitTab(kpis)
                2 -> ExecutiveScorecardTab(scorecard)
                3 -> ExecutiveRankingsTab(rankings)
                4 -> ExecutiveDriversAndLeakageTab(drivers, leakage)
                5 -> ExecutivePrioritiesTab(priorities)
                6 -> ExecutiveConcentrationTab(concentration)
                7 -> ExecutiveReconciliationTab(reconciliation, snapshot)
                8 -> ExecutiveReportTab(report)
            }
        }
    }
}

@Composable
fun ExecutiveOverviewTab(
    snapshot: ExecutiveProfitabilitySnapshotDto?,
    scorecard: ExecutiveManagementScorecardDto?,
    priorities: List<ExecutivePriorityItemDto>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enterprise Profitability Health", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        val health = scorecard?.classification ?: snapshot?.overallHealth ?: "HEALTHY"
                        HealthBadge(health)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        scorecard?.executiveSummary ?: "Snapshot calculated with comprehensive canonical ledger attribution.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Scorecard Rating", style = MaterialTheme.typography.labelSmall)
                            Text("${scorecard?.overallScore ?: snapshot?.overallScore ?: "85.0000"}/100", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Column {
                            Text("Active Alerts", style = MaterialTheme.typography.labelSmall)
                            Text("${snapshot?.activeAlertsCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.error)
                        }
                        Column {
                            Text("Decisions Pending", style = MaterialTheme.typography.labelSmall)
                            Text("${priorities.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        item {
            Text("Core Financial Aggregates", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricSummaryCard(title = "Gross Revenue", value = "BDT ${snapshot?.totalGrossRevenue ?: "0.0000"}", modifier = Modifier.weight(1f))
                MetricSummaryCard(title = "Actual Cost", value = "BDT ${snapshot?.totalActualCost ?: "0.0000"}", modifier = Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricSummaryCard(title = "Gross Profit", value = "BDT ${snapshot?.totalGrossProfit ?: "0.0000"}", modifier = Modifier.weight(1f))
                MetricSummaryCard(title = "Gross Margin", value = "${snapshot?.grossMarginPercentage ?: "0.0000"}%", modifier = Modifier.weight(1f))
            }
        }

        if (snapshot?.forecastRevenue != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Forecast Projection (Next Horizon)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Projected Rev: BDT ${snapshot.forecastRevenue}", style = MaterialTheme.typography.bodySmall)
                            Text("Projected GP: BDT ${snapshot.forecastGrossProfit}", style = MaterialTheme.typography.bodySmall)
                            Text("Margin: ${snapshot.forecastGrossMargin}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveKpiCockpitTab(kpis: List<ExecutiveKpiDto>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(kpis) { kpi ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kpi.kpiName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Category: ${kpi.category}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        HealthBadge(kpi.health)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Current Value", style = MaterialTheme.typography.labelSmall)
                            Text("${kpi.currentValue} ${kpi.unit}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        val varPct = kpi.variancePercentage
                        if (varPct != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Variance", style = MaterialTheme.typography.labelSmall)
                                Text("${if (varPct.startsWith("-")) "" else "+"}$varPct%", fontWeight = FontWeight.Bold, color = if (kpi.direction == "IMPROVING") Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(kpi.explanation, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ExecutiveScorecardTab(scorecard: ExecutiveManagementScorecardDto?) {
    if (scorecard == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No scorecard calculated.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Management Scorecard Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Overall Score: ${scorecard.overallScore}/100.0000 (${scorecard.classification})", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(scorecard.executiveSummary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(scorecard.items) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.dimensionName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        HealthBadge(item.classification)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Weight: ${(item.weight.toDoubleOrNull() ?: 0.0) * 100}%", style = MaterialTheme.typography.bodySmall)
                        Text("Raw Score: ${item.rawScore}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Weighted: ${item.weightedScore}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if (item.keyFindings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• ${item.keyFindings.joinToString("; ")}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveRankingsTab(rankings: ExecutiveRankingsPayloadDto?) {
    if (rankings == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No ranking intelligence available.")
        }
        return
    }

    var rankingTab by remember { mutableIntStateOf(0) }
    val rankingTabs = listOf("Top Jobs", "Loss Jobs", "Top Products", "Top Customers", "Vendors")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = rankingTab) {
            rankingTabs.forEachIndexed { idx, title ->
                Tab(selected = rankingTab == idx, onClick = { rankingTab = idx }, text = { Text(title, fontSize = 12.sp) })
            }
        }

        val itemsToDisplay = when (rankingTab) {
            0 -> rankings.topProfitableJobs
            1 -> rankings.lossMakingJobs
            2 -> rankings.topProfitableProducts
            3 -> rankings.topContributingCustomers
            else -> rankings.highestSpendVendors
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itemsToDisplay) { rankItem ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${rankItem.rank}", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(36.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${rankItem.entityName} (${rankItem.entityCode})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Rev: BDT ${rankItem.revenue} | Cost: BDT ${rankItem.cost}", style = MaterialTheme.typography.bodySmall)
                            Text(rankItem.highlightReason, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("BDT ${rankItem.grossProfit}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (rankItem.grossProfit.startsWith("-")) Color.Red else Color(0xFF2E7D32))
                            Text("${rankItem.marginPercentage}%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExecutiveDriversAndLeakageTab(
    drivers: List<ExecutiveProfitabilityDriverDto>,
    leakage: ExecutiveLeakageSummaryDto?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Profitability Leakage Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Total Detected Leakage: BDT ${leakage?.totalLeakageAmount ?: "0.0000"}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                    Text(leakage?.primaryMitigationRecommendation ?: "No leakages identified.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Text("Key Profitability Drivers", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        items(drivers) { driver ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(driver.driverName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Impact: BDT ${driver.impactAmount} (${driver.impactPercentage}%)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(driver.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ExecutivePrioritiesTab(priorities: List<ExecutivePriorityItemDto>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(priorities) { prio ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${prio.priorityRank}", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(30.dp))
                            Text(prio.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        HealthBadge(prio.severity)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Category: ${prio.category}", style = MaterialTheme.typography.bodySmall)
                        Text("Financial Impact: BDT ${prio.financialImpact}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Priority: ${prio.priorityScore}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Action: ${prio.recommendedActionTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun ExecutiveConcentrationTab(concentration: ExecutiveConcentrationSummaryDto?) {
    if (concentration == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No concentration data available.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enterprise Concentration Risk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        HealthBadge(concentration.overallConcentrationRisk)
                    }
                }
            }
        }

        item {
            ConcentrationCard(title = "Customer Revenue Concentration", metric = concentration.customerRevenueConcentration)
        }
        item {
            ConcentrationCard(title = "Customer Profit Concentration", metric = concentration.customerProfitConcentration)
        }
        item {
            ConcentrationCard(title = "Product Revenue Concentration", metric = concentration.productRevenueConcentration)
        }
        item {
            ConcentrationCard(title = "Vendor Spend Concentration", metric = concentration.vendorSpendConcentration)
        }
    }
}

@Composable
fun ConcentrationCard(title: String, metric: ConcentrationMetricDto) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HealthBadge(metric.riskLevel)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Top 1: ${metric.top1SharePercentage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Top 5: ${metric.top5SharePercentage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Top 10: ${metric.top10SharePercentage}%", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Entities: ${metric.totalEntitiesCount}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(metric.explanation, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        }
    }
}

@Composable
fun ExecutiveReconciliationTab(
    reconciliation: ExecutiveReconciliationResultDto?,
    snapshot: ExecutiveProfitabilitySnapshotDto?
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val balanced = reconciliation?.isBalanced == true
            Card(
                colors = CardDefaults.cardColors(containerColor = if (balanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (balanced) "✓ Reconciliation Balanced & Cryptographically Verified" else "⚠ Discrepancies Found in Reconciliation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (balanced) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reconciliation ID: ${reconciliation?.reconciliationId ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                    Text("Integrity Hash: ${snapshot?.integrityHash ?: reconciliation?.integrityHash ?: "N/A"}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Identity Verification Checks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReconCheckRow("Revenue Attribution Matches Job Actuals", reconciliation?.revenueMatches == true)
                    ReconCheckRow("Cost Attribution Matches Expense Ledgers", reconciliation?.costMatches == true)
                    ReconCheckRow("Gross Profit Mathematical Identity (GP = Rev - Cost)", reconciliation?.profitMatches == true)
                    ReconCheckRow("Forecast Projections Match Statistical Snapshot", reconciliation?.forecastMatches == true)
                    ReconCheckRow("Active Alert Counts Match Monitoring Engine", reconciliation?.alertCountsMatch == true)
                }
            }
        }
    }
}

@Composable
fun ReconCheckRow(label: String, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(if (passed) "MATCH" else "MISMATCH", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (passed) Color(0xFF2E7D32) else Color(0xFFC62828))
    }
}

@Composable
fun ExecutiveReportTab(report: ExecutiveProfitabilityReportDto?) {
    if (report == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Report not yet generated.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Executive Profitability Report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Version: ${report.contractVersion} | Generated: ${report.generatedAt}", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(report.executiveSummary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        items(report.sections) { section ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(section.sectionTitle, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(section.summaryNarrative, style = MaterialTheme.typography.bodySmall)
                    if (section.keyMetrics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        section.keyMetrics.forEach { (k, v) ->
                            Text("$k: $v", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricSummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun HealthBadge(health: String) {
    val (bgColor, textColor) = when (health) {
        "EXCELLENT" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "HEALTHY", "LOW" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "STABLE", "MEDIUM" -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        "WATCH" -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        "WARNING", "HIGH" -> Color(0xFFFBE9E7) to Color(0xFFD84315)
        "CRITICAL" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        else -> Color(0xFFEEEEEE) to Color(0xFF616161)
    }

    Surface(
        color = bgColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = health,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
