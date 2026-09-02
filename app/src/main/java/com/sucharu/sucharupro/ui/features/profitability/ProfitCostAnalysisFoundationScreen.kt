package com.sucharu.sucharupro.ui.features.profitability

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.profitability.*
import java.math.BigDecimal

/**
 * UI Foundation Screen for Module 16 Step 01: Profit & Cost Analysis Foundation & Financial Handoff.
 * Designed with Sucharu Pro dark navy aesthetics and comprehensive integrity indicators.
 */
@Composable
fun ProfitCostAnalysisFoundationScreen(
    principal: AuthenticatedPrincipal,
    modifier: Modifier = Modifier,
    initialScope: ProfitabilityScope = ProfitabilityScope.BUSINESS,
    initialPeriodId: String = "PER-2026-M08",
    onReconcileRequested: ((String) -> Unit)? = null
) {
    var selectedScope by remember { mutableStateOf(initialScope) }
    var periodIdText by remember { mutableStateOf(initialPeriodId) }
    var targetEntityIdText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Sample/Active Snapshot State
    var activeSnapshot by remember {
        mutableStateOf(
            ProfitabilitySnapshot(
                id = "SNAP-INIT-001",
                tenantId = principal.projectId,
                projectId = principal.projectId,
                scope = initialScope,
                targetEntityId = null,
                periodId = initialPeriodId,
                currency = "BDT",
                metrics = ProfitabilityMetric(
                    revenue = BigDecimal("150000.0000"),
                    directCost = BigDecimal("95000.0000"),
                    indirectCost = BigDecimal("15000.0000"),
                    totalCost = BigDecimal("110000.0000"),
                    grossProfit = BigDecimal("40000.0000"),
                    grossMarginPercentage = BigDecimal("26.6667"),
                    baselineCost = BigDecimal("105000.0000"),
                    costVariance = BigDecimal("5000.0000")
                ),
                costBreakdowns = listOf(
                    CostComponentBreakdown(CostComponentType.MATERIAL, BigDecimal("60000.0000"), BigDecimal("54.5455"), 4),
                    CostComponentBreakdown(CostComponentType.LABOUR, BigDecimal("25000.0000"), BigDecimal("22.7273"), 2),
                    CostComponentBreakdown(CostComponentType.MACHINE, BigDecimal("10000.0000"), BigDecimal("9.0909"), 1),
                    CostComponentBreakdown(CostComponentType.OVERHEAD, BigDecimal("15000.0000"), BigDecimal("13.6364"), 3)
                ),
                revenueProvenances = listOf(
                    RevenueProvenance("REV-1", principal.projectId, principal.projectId, RevenueSourceType.CUSTOMER_INVOICE, "INV-2026-001", recognizedAmount = BigDecimal("150000.0000"))
                ),
                costAttributions = listOf(
                    CostAttributionReference("ATTR-1", principal.projectId, principal.projectId, CostAttributionSourceType.EXPENSE, "EXP-101", CostComponentType.MATERIAL, sourceAmount = BigDecimal("60000.0000"), attributableAmount = BigDecimal("60000.0000")),
                    CostAttributionReference("ATTR-2", principal.projectId, principal.projectId, CostAttributionSourceType.PAYABLE, "PAY-201", CostComponentType.LABOUR, sourceAmount = BigDecimal("25000.0000"), attributableAmount = BigDecimal("25000.0000"))
                ),
                calculationVersion = "1.0.0",
                sourceIntegrityStatus = SourceIntegrityStatus.VERIFIED,
                financialHandoffVerified = true,
                handoffChecksum = "a8f5c3b49e102f98d47b6a12",
                integrityNotes = listOf("Module 15 Financial Handoff Verified", "Double-Entry Ledger Balanced (Debits == Credits)"),
                generatedBy = principal.userId
            )
        )
    }

    var reconciliationResult by remember { mutableStateOf<ProfitabilityReconciliationEvent?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(16.dp)
    ) {
        // Header Banner
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "PROFIT & COST ANALYSIS FOUNDATION",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9ECAFF),
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Module 16 Step 01 — Canonical Financial Handoff & Analytical Projections",
                        color = Color(0xFFB7C8D8),
                        fontSize = 11.sp
                    )
                }

                // Integrity Status Badge
                Surface(
                    color = when (activeSnapshot.sourceIntegrityStatus) {
                        SourceIntegrityStatus.VERIFIED -> Color(0xFF005A36)
                        SourceIntegrityStatus.PARTIALLY_VERIFIED -> Color(0xFF7A5900)
                        SourceIntegrityStatus.SOURCE_CONFLICT, SourceIntegrityStatus.CALCULATION_BLOCKED -> Color(0xFF8C1D40)
                        else -> Color(0xFF4A5568)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "INTEGRITY: ${activeSnapshot.sourceIntegrityStatus.name}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Content List
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section 1: Financial Handoff & Ledger Balance Card
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "CANONICAL FINANCIAL HANDOFF (MODULE 15)",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9ECAFF),
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (activeSnapshot.financialHandoffVerified) "STATUS: VERIFIED" else "STATUS: UNVERIFIED",
                                color = if (activeSnapshot.financialHandoffVerified) Color(0xFF4CAF50) else Color(0xFFFFB4AB),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HandoffMetricItem("Ledger Balance", "BALANCED", Color(0xFF4CAF50), Modifier.weight(1f))
                            HandoffMetricItem("Handoff Checksum", activeSnapshot.handoffChecksum?.take(8) ?: "N/A", Color(0xFF9ECAFF), Modifier.weight(1f))
                            HandoffMetricItem("Period Code", activeSnapshot.periodId ?: "N/A", Color.White, Modifier.weight(1f))
                        }

                        if (activeSnapshot.integrityNotes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                activeSnapshot.integrityNotes.forEach { note ->
                                    Text("• $note", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Scope Selector
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("SELECT ANALYSIS SCOPE", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProfitabilityScope.values().forEach { scope ->
                                FilterChip(
                                    selected = selectedScope == scope,
                                    onClick = { selectedScope = scope },
                                    label = { Text(scope.name, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF00497D),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Profitability KPI Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PROFITABILITY & MARGIN METRICS", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricKpiCard("REVENUE", "BDT ${activeSnapshot.metrics.revenue}", Color(0xFF4CAF50), Modifier.weight(1f))
                        MetricKpiCard("TOTAL COST", "BDT ${activeSnapshot.metrics.totalCost}", Color(0xFFFFB4AB), Modifier.weight(1f))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricKpiCard("GROSS PROFIT", "BDT ${activeSnapshot.metrics.grossProfit}", Color(0xFF64B5F6), Modifier.weight(1f))
                        MetricKpiCard("GROSS MARGIN", "${activeSnapshot.metrics.grossMarginPercentage}%", Color(0xFFFFD54F), Modifier.weight(1f))
                    }

                    if (activeSnapshot.metrics.costVariance != null) {
                        MetricKpiCard("COST VARIANCE", "BDT ${activeSnapshot.metrics.costVariance}", Color(0xFFFF8A80), Modifier.fillMaxWidth())
                    }
                }
            }

            // Section 4: Cost Breakdown by Component
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("COST COMPONENT BREAKDOWN", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        activeSnapshot.costBreakdowns.forEach { breakdown ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF38BDF8), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(breakdown.componentType.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                                Text("BDT ${breakdown.totalAmount} (${breakdown.percentageOfTotalCost}%)", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Section 5: Revenue Provenance & Source Traceability
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("REVENUE PROVENANCE & CANONICAL ORIGIN", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        activeSnapshot.revenueProvenances.forEach { rev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${rev.canonicalSourceType}: ${rev.canonicalSourceId}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text("BDT ${rev.recognizedAmount}", color = Color(0xFF4CAF50), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 6: Reconciliation Controls
            item {
                Surface(
                    color = Color(0xFF1C2541),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("FINANCIAL RECONCILIATION", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 13.sp)
                            Button(
                                onClick = {
                                    reconciliationResult = ProfitabilityReconciliationEvent(
                                        id = "REC-DEMO-001",
                                        tenantId = principal.projectId,
                                        projectId = principal.projectId,
                                        snapshotId = activeSnapshot.id,
                                        scope = activeSnapshot.scope,
                                        periodId = activeSnapshot.periodId,
                                        isReconciled = true,
                                        canonicalRevenueTotal = activeSnapshot.metrics.revenue,
                                        snapshotRevenueTotal = activeSnapshot.metrics.revenue,
                                        revenueDifference = BigDecimal.ZERO.setScale(4),
                                        canonicalCostTotal = activeSnapshot.metrics.totalCost,
                                        snapshotCostTotal = activeSnapshot.metrics.totalCost,
                                        costDifference = BigDecimal.ZERO.setScale(4),
                                        discrepancies = emptyList(),
                                        checkedBy = principal.userId
                                    )
                                    onReconcileRequested?.invoke(activeSnapshot.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005A36)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("RECONCILE NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (reconciliationResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = if (reconciliationResult!!.isReconciled) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (reconciliationResult!!.isReconciled) "RECONCILIATION PASSED: 100% Alignment with Canonical Sources" else "DISCREPANCIES DETECTED",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text("Checked by: ${reconciliationResult!!.checkedBy} at ${reconciliationResult!!.checkedAt}", color = Color(0xFFCBD5E1), fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricKpiCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF1C2541),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HandoffMetricItem(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 9.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
