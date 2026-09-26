package com.sucharu.sucharupro.ui.admin.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.service.jobcosting.JobCostingFoundationService
import com.sucharu.sucharupro.ui.admin.components.AdminCard
import com.sucharu.sucharupro.ui.admin.components.AdminKpiCard
import com.sucharu.sucharupro.ui.admin.components.AdminSection
import com.sucharu.sucharupro.ui.admin.theme.AdminTheme
import com.sucharu.sucharupro.ui.admin.workspace.AdminModuleWorkspaceContainer
import com.sucharu.sucharupro.ui.navigation.AppDestination

/**
 * Unified Admin Workspace for BI-02-A Printing Job Costing Foundation & Cost Visibility (Module 04 / 18 / 16).
 */
@Composable
fun AdminPrintingJobCostingScreen(
    principal: AuthenticatedPrincipal?,
    modifier: Modifier = Modifier,
    onNavigateToDestination: (AppDestination) -> Unit = {}
) {
    val service = remember { JobCostingFoundationService() }
    val summary = remember { service.buildJobCostIntelligenceSummary() }

    AdminModuleWorkspaceContainer(
        title = "BI-02 Printing Job Costing & Gross Margin Visibility",
        subtitle = "Estimated vs Actual Printing Cost, Variance Analysis & Profitability Handoff",
        icon = Icons.Default.Calculate,
        canonicalModule = "Module 04 / 18 / 16",
        requiredCapability = "REPORT_VIEW_PRODUCTION",
        principal = principal,
        modifier = modifier
    ) {
        // KPI Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AdminTheme.spacing.md)
        ) {
            AdminKpiCard(
                title = "Total Estimated Cost",
                value = "৳${summary.totalEstimatedCostSum.toInt()}",
                trendDeltaPercentage = 0.0,
                subtitle = "Quotation Budgeted Baseline",
                icon = Icons.Default.Calculate,
                accentColor = AdminTheme.colors.primaryText,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Total Actual Job Cost",
                value = "৳${summary.totalActualCostSum.toInt()}",
                trendDeltaPercentage = 3.2,
                subtitle = "Net Variance: +৳${summary.netCostVarianceSum.toInt()}",
                icon = Icons.Default.Engineering,
                accentColor = if (summary.netCostVarianceSum > java.math.BigDecimal.ZERO) AdminTheme.colors.warning else AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
            AdminKpiCard(
                title = "Avg Gross Margin %",
                value = "${summary.averageGrossMarginPercentage}%",
                trendDeltaPercentage = 5.4,
                subtitle = "Sales Revenue: ৳${summary.totalSellingPriceSnapshotSum.toInt()}",
                icon = Icons.Default.TrendingUp,
                accentColor = AdminTheme.colors.success,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(AdminTheme.spacing.lg))

        // Job Cost Variance Intelligence
        AdminSection(
            title = "Printing Job Cost Breakdown & Variance Intelligence",
            subtitle = "Job-level cost overruns, actual vs estimated breakdown and gross margin"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                summary.jobBreakdowns.forEach { job ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = when (job.varianceClassification.name) {
                                            "COST_OVER_ESTIMATE" -> Color(0xFFF59E0B)
                                            "COST_UNDER_ESTIMATE" -> Color(0xFF10B981)
                                            else -> Color(0xFF0284C7)
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(text = job.varianceClassification.name, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Job #${job.jobId} (Order #${job.orderId})", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(text = "Margin: ${job.grossMarginPercentage}%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF10B981))
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = job.productName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(text = "Customer: ${job.customerName} • Qty: ${job.orderQuantity.toInt()} Pcs", fontSize = 11.sp, color = Color(0xFF94A3B8))

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(color = Color(0xFF0F172A), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Estimated: ৳${job.estimatedTotalCost.toInt()}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text(text = "Actual: ৳${job.actualTotalCost.toInt()}", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "Variance: ${if (job.costVariance > java.math.BigDecimal.ZERO) "+" else ""}৳${job.costVariance.toInt()} (${job.costVariancePercentage}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (job.costVariance > java.math.BigDecimal.ZERO) Color(0xFFF59E0B) else Color(0xFF10B981)
                                    )
                                    Text(text = "Selling Price: ৳${job.sellingPriceSnapshot.toInt()}", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
