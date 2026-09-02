package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.*

@Composable
fun HealthScoreCard(
    healthScore: FinancialHealthScore,
    modifier: Modifier = Modifier
) {
    val statusColor = when (healthScore.status) {
        FinancialHealthStatus.EXCELLENT,
        FinancialHealthStatus.HEALTHY -> Color(0xFF34D399)
        FinancialHealthStatus.WATCH -> Color(0xFFFBBF24)
        FinancialHealthStatus.AT_RISK,
        FinancialHealthStatus.CRITICAL -> Color(0xFFF87171)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("FINANCIAL HEALTH SCORE", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(healthScore.status.defaultLabel, color = statusColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        .border(2.dp, statusColor, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${healthScore.score}", color = Color(0xFFF8FAFC), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF334155), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                HealthDimensionPill(label = "Liquidity", score = healthScore.liquidityScore)
                HealthDimensionPill(label = "Profit", score = healthScore.profitabilityScore)
                HealthDimensionPill(label = "Receivables", score = healthScore.receivableHealthScore)
                HealthDimensionPill(label = "Payables", score = healthScore.payableHealthScore)
            }
        }
    }
}

@Composable
fun HealthDimensionPill(label: String, score: Int) {
    val color = when {
        score >= 75 -> Color(0xFF34D399)
        score >= 50 -> Color(0xFFFBBF24)
        else -> Color(0xFFF87171)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 10.sp)
        Text("$score", color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExecutiveKpiCard(
    title: String,
    value: Money,
    subtitle: String? = null,
    trend: FinancialKpiTrend? = null,
    accentColor: Color = Color(0xFF38BDF8),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("${value.formatted()} BDT", color = accentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, color = Color(0xFF64748B), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun RiskAlertCard(
    risk: FinancialRiskIndicator,
    modifier: Modifier = Modifier
) {
    val color = when (risk.severity) {
        FinancialRiskSeverity.INFO -> Color(0xFF38BDF8)
        FinancialRiskSeverity.LOW -> Color(0xFF34D399)
        FinancialRiskSeverity.MEDIUM -> Color(0xFFFBBF24)
        FinancialRiskSeverity.HIGH,
        FinancialRiskSeverity.CRITICAL -> Color(0xFFF87171)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(risk.title, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = color.copy(alpha = 0.2f)
                ) {
                    Text(
                        risk.severity.defaultLabel,
                        color = color,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(risk.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

@Composable
fun AnomalyCard(
    anomaly: FinancialAnomaly,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF87171).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(anomaly.title, color = Color(0xFFF8FAFC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(anomaly.type.defaultLabel, color = Color(0xFFFBBF24), fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(anomaly.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
        }
    }
}

@Composable
fun GovernanceControlRow(
    control: AnalyticsControlResult,
    modifier: Modifier = Modifier
) {
    val color = when (control.status) {
        FinancialGovernanceStatus.PASSED -> Color(0xFF34D399)
        FinancialGovernanceStatus.WARNING -> Color(0xFFFBBF24)
        FinancialGovernanceStatus.CONTROL_EXCEPTION,
        FinancialGovernanceStatus.CRITICAL_EXCEPTION -> Color(0xFFF87171)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(control.title, color = Color(0xFFF8FAFC), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(control.description, color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.2f)
            ) {
                Text(
                    control.status.defaultLabel,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
