package com.sucharu.sucharupro.ui.features.qc.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.qc.analytics.QcAnalyticsSummary
import com.sucharu.sucharupro.domain.model.qc.analytics.QcInsightSeverity
import com.sucharu.sucharupro.domain.model.qc.analytics.QcOperationalInsight
import com.sucharu.sucharupro.domain.model.qc.analytics.QcPeriodType
import com.sucharu.sucharupro.ui.components.AppCard

@Composable
fun QcAnalyticsDashboardScreen(
    viewModel: QcAnalyticsDashboardViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "QC Cost & Time Analytics",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }

                // Period Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QcPeriodType.values().forEach { periodType ->
                        FilterChip(
                            selected = uiState.selectedPeriodType == periodType,
                            onClick = { viewModel.setPeriodType(periodType) },
                            label = { Text(periodType.defaultLabel) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab.ordinal,
                    edgePadding = 0.dp
                ) {
                    QcAnalyticsTab.values().forEach { tab ->
                        Tab(
                            selected = uiState.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                when (uiState.selectedTab) {
                    QcAnalyticsTab.OVERVIEW -> {
                        OverviewTabContent(
                            summary = uiState.summary,
                            insights = uiState.insights
                        )
                    }
                    QcAnalyticsTab.JOBS -> {
                        QcJobAnalyticsScreen(jobs = uiState.jobAnalytics)
                    }
                    QcAnalyticsTab.DEFECTS -> {
                        QcDefectAnalyticsScreen(defects = uiState.defectAnalytics)
                    }
                    QcAnalyticsTab.STAGES -> {
                        QcStageAnalyticsScreen(stages = uiState.stageAnalytics)
                    }
                    QcAnalyticsTab.INSIGHTS -> {
                        OperationalInsightsContent(insights = uiState.insights)
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    summary: QcAnalyticsSummary?,
    insights: List<QcOperationalInsight>
) {
    if (summary == null) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-level KPI grid
        Text(
            text = "Quality & Operational Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Total QC Cost",
                value = "${String.format("%.2f", summary.totalQcCost)} BDT",
                subtitle = "Avg: ${String.format("%.2f", summary.averageQcCostPerJob)} BDT/Job",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Total QC Time",
                value = "${summary.totalQcTimeMinutes} mins",
                subtitle = "Avg: ${String.format("%.1f", summary.averageQcTimeMinutesPerJob)} mins/Job",
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "Cost Variance",
                value = "${if (summary.totalCostVariance > 0) "+" else ""}${String.format("%.2f", summary.totalCostVariance)} BDT",
                subtitle = if (summary.totalCostVariance > 0) "Budget Overrun" else "Within Budget",
                valueColor = if (summary.totalCostVariance > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Time Variance",
                value = "${if (summary.totalTimeVariance > 0) "+" else ""}${summary.totalTimeVariance} mins",
                subtitle = if (summary.totalTimeVariance > 0) "Schedule Overrun" else "On Schedule",
                valueColor = if (summary.totalTimeVariance > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "Quality Process Rates",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                title = "First-Pass Rate",
                value = "${String.format("%.1f", summary.firstPassQcRate)}%",
                subtitle = "Clean 1st Pass",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Rework Rate",
                value = "${String.format("%.1f", summary.reworkRate)}%",
                subtitle = "${summary.totalReworks} Reworks",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Re-QC Rate",
                value = "${String.format("%.1f", summary.reQcRate)}%",
                subtitle = "${summary.totalReQcCycles} Cycles",
                modifier = Modifier.weight(1f)
            )
        }

        // Active Volume Summary
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Operational Pipeline Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("• Total Inspected Jobs: ${summary.totalJobs}")
                Text("• Open Defects: ${summary.openDefectCount} / Total: ${summary.totalDefects}")
                Text("• Active Reworks: ${summary.activeReworkCount}")
                Text("• Failed Re-QC Cycles: ${summary.failedReQcCount}")
                Text("• Released Production Jobs: ${summary.releasedJobCount}")
            }
        }

        // Operational Insights Preview
        if (insights.isNotEmpty()) {
            Text(
                text = "Key Operational Insights (${insights.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            insights.take(3).forEach { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InsightCard(insight: QcOperationalInsight) {
    val (bgColor, iconColor) = when (insight.severity) {
        QcInsightSeverity.CRITICAL -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        QcInsightSeverity.WARNING -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        QcInsightSeverity.INFO -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (insight.severity == QcInsightSeverity.CRITICAL) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null,
                tint = iconColor
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = insight.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = iconColor)
                Text(text = insight.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF212121))
            }
        }
    }
}

@Composable
private fun OperationalInsightsContent(insights: List<QcOperationalInsight>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("All Operational Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (insights.isEmpty()) {
            Text("No operational warnings or anomalies detected.", style = MaterialTheme.typography.bodyMedium)
        } else {
            insights.forEach { insight ->
                InsightCard(insight = insight)
            }
        }
    }
}
