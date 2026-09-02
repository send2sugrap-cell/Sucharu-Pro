package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalSettlementWorkspaceScreen(
    workspace: VendorPortalFinancialWorkspaceDto,
    onViewSettlementsClick: () -> Unit = {},
    onViewReconciliationsClick: () -> Unit = {},
    onViewDisputesClick: () -> Unit = {},
    onViewPaymentsClick: () -> Unit = {},
    onViewEvidenceClick: () -> Unit = {},
    onViewThreadsClick: () -> Unit = {},
    onViewAnalyticsClick: () -> Unit = {},
    onViewActivityClick: () -> Unit = {},
    onSettlementClick: (String) -> Unit = {},
    onReconciliationClick: (String) -> Unit = {},
    onDisputeClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vendor Settlement & Reconciliation Workspace",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Overview Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FinancialSummaryMetricCard(
                        title = "Settled Total",
                        value = "${String.format("%,.2f", workspace.analytics.totalSettledAmount)} ${workspace.analytics.currency}",
                        subtitle = "Cycle: ${String.format("%.1f", workspace.analytics.averageSettlementCycleDays)} days",
                        indicatorColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    FinancialSummaryMetricCard(
                        title = "Outstanding",
                        value = "${String.format("%,.2f", workspace.outstandingBalance)} ${workspace.analytics.currency}",
                        subtitle = "Unsettled Payables",
                        indicatorColor = if (workspace.outstandingBalance > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    FinancialSummaryMetricCard(
                        title = "Active Disputes",
                        value = "${workspace.analytics.activeDisputeCount}",
                        subtitle = "Resolution: ${String.format("%.0f", workspace.analytics.disputeResolutionRate)}%",
                        indicatorColor = if (workspace.analytics.activeDisputeCount > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    FinancialSummaryMetricCard(
                        title = "Pending Recon",
                        value = "${workspace.analytics.pendingReconciliationCount}",
                        subtitle = "Cases Open",
                        indicatorColor = if (workspace.analytics.pendingReconciliationCount > 0) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Navigation Action Chips
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Financial Collaboration Modules",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onViewSettlementsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Settlements", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onViewReconciliationsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reconciliation", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onViewDisputesClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disputes", fontSize = 12.sp)
                            }
                            Button(
                                onClick = onViewPaymentsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Payments", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onViewEvidenceClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Evidence Vault", fontSize = 12.sp, color = Color(0xFF93C5FD))
                            }
                            OutlinedButton(
                                onClick = onViewThreadsClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discussions", fontSize = 12.sp, color = Color(0xFFC7D2FE))
                            }
                            OutlinedButton(
                                onClick = onViewAnalyticsClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Analytics", fontSize = 12.sp, color = Color(0xFF6EE7B7))
                            }
                            OutlinedButton(
                                onClick = onViewActivityClick,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Audit Log", fontSize = 12.sp, color = Color(0xFFFCD34D))
                            }
                        }
                    }
                }
            }

            // Recent Settlements Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Settlements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onViewSettlementsClick) {
                        Text("View All (${workspace.settlementOverview.size})", color = Color(0xFF60A5FA))
                    }
                }
            }

            if (workspace.settlementOverview.isEmpty()) {
                item {
                    EmptySectionCard("No settlement records found.")
                }
            } else {
                items(workspace.settlementOverview.take(3)) { settlement ->
                    SettlementOverviewCard(
                        settlement = settlement,
                        onClick = { onSettlementClick(settlement.settlementId) }
                    )
                }
            }

            // Pending Reconciliations Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pending Reconciliation Inquiries",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onViewReconciliationsClick) {
                        Text("View All (${workspace.pendingReconciliations.size})", color = Color(0xFF818CF8))
                    }
                }
            }

            if (workspace.pendingReconciliations.isEmpty()) {
                item {
                    EmptySectionCard("All reconciliations are up to date.")
                }
            } else {
                items(workspace.pendingReconciliations.take(3)) { recon ->
                    ReconciliationCaseOverviewCard(
                        case = recon,
                        onClick = { onReconciliationClick(recon.caseId) }
                    )
                }
            }

            // Open Disputes Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Financial Disputes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    TextButton(onClick = onViewDisputesClick) {
                        Text("View All (${workspace.openDisputes.size})", color = Color(0xFFF87171))
                    }
                }
            }

            if (workspace.openDisputes.isEmpty()) {
                item {
                    EmptySectionCard("No open financial disputes.")
                }
            } else {
                items(workspace.openDisputes.take(3)) { dispute ->
                    FinancialDisputeOverviewCard(
                        dispute = dispute,
                        onClick = { onDisputeClick(dispute.disputeId) }
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialSummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(text = title, fontSize = 12.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun SettlementOverviewCard(
    settlement: VendorPortalSettlementSummaryDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
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
                    text = settlement.settlementNumber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Method: ${settlement.settlementMethod} | Allocations: ${settlement.allocationCount}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%,.2f", settlement.netPayable)} ${settlement.currency}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = settlement.status)
            }
        }
    }
}

@Composable
fun ReconciliationCaseOverviewCard(
    case: VendorPortalReconciliationCaseDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
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
                    text = case.caseNumber,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = case.subject,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Variance: ${String.format("%,.2f", case.varianceAmount)} ${case.currency}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = case.status)
            }
        }
    }
}

@Composable
fun FinancialDisputeOverviewCard(
    dispute: VendorPortalFinancialDisputeDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp)
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
                    text = "${dispute.disputeNumber} (${dispute.category})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = dispute.reason,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Disputed: ${String.format("%,.2f", dispute.disputedAmount)} ${dispute.currency}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = dispute.status)
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "SETTLED", "RESOLVED", "ACKNOWLEDGED", "PAID" -> Color(0xFF065F46) to Color(0xFF34D399)
        "APPROVED", "PROCESSING" -> Color(0xFF1E40AF) to Color(0xFF60A5FA)
        "OPEN", "SUBMITTED", "DRAFT", "PENDING" -> Color(0xFF78350F) to Color(0xFFFBBF24)
        "UNDER_REVIEW", "RESPONSE_REQUIRED", "INTERNAL_RESPONSE_REQUIRED", "VENDOR_RESPONSE_REQUIRED" -> Color(0xFF4C1D95) to Color(0xFFA78BFA)
        "DISPUTED", "REJECTED", "DECLINED", "CANCELLED" -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        else -> Color(0xFF334155) to Color(0xFF94A3B8)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun EmptySectionCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message, color = Color(0xFF64748B), fontSize = 13.sp)
        }
    }
}
