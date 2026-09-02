package com.sucharu.sucharupro.ui.features.qc.governance

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.governance.QcAlertSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcKpiThreshold
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdSeverity
import com.sucharu.sucharupro.domain.model.qc.governance.QcThresholdStatus

/**
 * Main Quality Governance & Continuous Quality Improvement Dashboard Screen (Module 06 Step 10).
 */
@Composable
fun QcGovernanceDashboardScreen(
    viewModel: QcGovernanceDashboardViewModel,
    onNavigateToAlerts: (String) -> Unit = {},
    onNavigateToActions: (String) -> Unit = {},
    onNavigateToReviews: (String) -> Unit = {},
    onNavigateToTargets: (String) -> Unit = {},
    onNavigateToSnapshots: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("KPI Overview", "Active Alerts", "Improvement Actions", "Quality Reviews")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quality Governance & CQI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Project: ${if (uiState.selectedProjectId.isNotBlank()) uiState.selectedProjectId else "All Projects"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score Badge
            QualityScoreBadge(score = uiState.overallQualityScore)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Executive Alert & KPI Breaches Bar
        ExecutiveAlertSummaryBar(
            criticalCount = uiState.criticalAlertCount,
            warningCount = uiState.warningAlertCount,
            openActionCount = uiState.openActionCount
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (selectedTab) {
                0 -> KpiEvaluationsList(uiState.kpiEvaluations)
                1 -> ActiveAlertsSection(uiState)
                2 -> ImprovementActionsSection(uiState)
                3 -> QualityReviewsSection(uiState)
            }
        }
    }
}

@Composable
fun QualityScoreBadge(score: Double) {
    val (bgColor, textColor) = when {
        score >= 90.0 -> Pair(Color(0xFF2E7D32), Color.White)
        score >= 75.0 -> Pair(Color(0xFFF57F17), Color.White)
        else -> Pair(Color(0xFFC62828), Color.White)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${String.format("%.1f", score)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = "Efficiency Score",
                fontSize = 11.sp,
                color = textColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ExecutiveAlertSummaryBar(
    criticalCount: Int,
    warningCount: Int,
    openActionCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMiniCard(
            title = "Critical Alerts",
            value = "$criticalCount",
            containerColor = if (criticalCount > 0) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
            textColor = if (criticalCount > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryMiniCard(
            title = "Warnings",
            value = "$warningCount",
            containerColor = if (warningCount > 0) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surfaceVariant,
            textColor = if (warningCount > 0) Color(0xFFF57F17) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        SummaryMiniCard(
            title = "Open Actions",
            value = "$openActionCount",
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SummaryMiniCard(
    title: String,
    value: String,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = title, fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun KpiEvaluationsList(evaluations: List<QcKpiThreshold>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(evaluations) { eval ->
            KpiThresholdItemCard(eval)
        }
    }
}

@Composable
fun KpiThresholdItemCard(eval: QcKpiThreshold) {
    val statusColor = when (eval.severity) {
        QcThresholdSeverity.INFO -> Color(0xFF2E7D32)
        QcThresholdSeverity.WARNING -> Color(0xFFF57F17)
        QcThresholdSeverity.CRITICAL -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eval.kpiType.defaultLabel,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = eval.message,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", eval.currentValue)} ${eval.kpiType.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = statusColor
                )
                Text(
                    text = "Target: ${String.format("%.1f", eval.targetValue)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveAlertsSection(uiState: QcGovernanceDashboardUiState) {
    if (uiState.activeAlerts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active quality alerts.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.activeAlerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = alert.title,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = alert.severity.defaultLabel,
                                fontWeight = FontWeight.Bold,
                                color = if (alert.severity == QcAlertSeverity.CRITICAL) Color(0xFFC62828) else Color(0xFFF57F17),
                                fontSize = 12.sp
                            )
                        }
                        Text(text = alert.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detected at: ${alert.detectedAt} | Status: ${alert.status.defaultLabel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImprovementActionsSection(uiState: QcGovernanceDashboardUiState) {
    if (uiState.improvementActions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No improvement actions registered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.improvementActions) { action ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = action.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = action.status.defaultLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(text = action.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Type: ${action.actionType.defaultLabel} | Priority: ${action.priority.defaultLabel}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QualityReviewsSection(uiState: QcGovernanceDashboardUiState) {
    if (uiState.reviews.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No quality reviews on record.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.reviews) { review ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = review.title, fontWeight = FontWeight.Bold)
                            Text(
                                text = review.status.defaultLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Reviewer: ${review.reviewerName ?: review.reviewerId} | Final QC Pass: ${review.finalQcPassRate}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
