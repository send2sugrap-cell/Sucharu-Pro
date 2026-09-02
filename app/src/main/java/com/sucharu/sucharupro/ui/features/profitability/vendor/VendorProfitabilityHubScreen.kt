package com.sucharu.sucharupro.ui.features.profitability.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.VendorProfitabilitySnapshotDto
import java.math.BigDecimal

/**
 * 1. Vendor Profitability & Supplier Economics Hub Screen
 */
@Composable
fun VendorProfitabilityHubScreen(
    snapshot: VendorProfitabilitySnapshotDto?,
    onNavigateToDetails: (String) -> Unit = {},
    onNavigateToCostBreakdown: (String) -> Unit = {},
    onNavigateToJobAttribution: (String) -> Unit = {},
    onNavigateToProductImpact: (String) -> Unit = {},
    onNavigateToCustomerImpact: (String) -> Unit = {},
    onNavigateToTrend: (String) -> Unit = {},
    onNavigateToEfficiency: (String) -> Unit = {},
    onNavigateToRisk: (String) -> Unit = {},
    onNavigateToDependency: () -> Unit = {},
    onNavigateToComparison: () -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToReconciliation: (String) -> Unit = {},
    onNavigateToProvenance: (String) -> Unit = {},
    onRecalculate: () -> Unit = {}
) {
    val bgNavy = Color(0xFF0F172A)
    val cardNavy = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)
    val accentEmerald = Color(0xFF10B981)
    val accentRose = Color(0xFFF43F5E)
    val accentAmber = Color(0xFFF59E0B)
    val accentIndigo = Color(0xFF6366F1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Vendor Profitability & Supplier Economics Hub",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Outsource Cost Control, Efficiency & Supplier Risk Intelligence",
            color = Color(0xFF94A3B8),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (snapshot != null) {
            // Vendor Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = snapshot.vendorName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Vendor ID: ${snapshot.vendorId} | Category: ${snapshot.serviceCategory ?: "GENERAL"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        val (riskBg, riskText) = when (snapshot.riskClassification) {
                            "LOW_RISK" -> Pair(Color(0xFF064E3B), accentEmerald)
                            "MODERATE_RISK" -> Pair(Color(0xFF78350F), accentAmber)
                            "HIGH_RISK", "CRITICAL_RISK" -> Pair(Color(0xFF881337), accentRose)
                            else -> Pair(Color(0xFF334155), Color.LightGray)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = riskBg,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = snapshot.riskClassification.replace("_", " "),
                                color = riskText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Key KPI Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        VendorKpiItem("Total Cost", "${snapshot.currency} ${snapshot.totalVendorCost}", accentRose)
                        VendorKpiItem("Paid Amount", "${snapshot.currency} ${snapshot.paidVendorCost}", accentEmerald)
                        VendorKpiItem("Outstanding", "${snapshot.currency} ${snapshot.outstandingExposure}", accentAmber)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        VendorKpiItem("Efficiency Score", "${snapshot.efficiencyScore}/100", accentCyan)
                        VendorKpiItem("Cost/Job", snapshot.costPerJob?.let { "${snapshot.currency} $it" } ?: "N/A", Color.White)
                        VendorKpiItem("Cost Share", snapshot.vendorCostSharePercentage?.let { "$it%" } ?: "N/A", accentIndigo)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Links
            Text(
                text = "Analytical Workspaces",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            VendorHubNavCard("Vendor Performance & Cost Breakdown", "12 cost components & outsource share") { onNavigateToCostBreakdown(snapshot.vendorId) }
            VendorHubNavCard("Job-Wise Outsource Attribution", "Jobs consuming vendor outsource operations") { onNavigateToJobAttribution(snapshot.vendorId) }
            VendorHubNavCard("Product-Wise Cost Impact", "Vendor cost share per product and unit economics") { onNavigateToProductImpact(snapshot.vendorId) }
            VendorHubNavCard("Customer-Wise Cost Attribution", "Customer orders impacted by vendor outsource") { onNavigateToCustomerImpact(snapshot.vendorId) }
            VendorHubNavCard("Cost Trajectory & Period Trends", "Directional trends, cost variance and forecast") { onNavigateToTrend(snapshot.vendorId) }
            VendorHubNavCard("Vendor Efficiency & Scoring", "Explainable scoring dimensions and factors") { onNavigateToEfficiency(snapshot.vendorId) }
            VendorHubNavCard("Supplier Risk & Exposure Assessment", "Disputes, rework, and quality burden") { onNavigateToRisk(snapshot.vendorId) }
            VendorHubNavCard("Dependency & Spend Concentration", "Top vendor spend analysis and dependency tiers") { onNavigateToDependency() }
            VendorHubNavCard("Multi-Vendor Comparative Analysis", "Side-by-side benchmarking against peers") { onNavigateToComparison() }
            VendorHubNavCard("Vendor Performance Ranking", "Rank by total cost, variance, and efficiency") { onNavigateToRanking() }
            VendorHubNavCard("Non-Mutating Financial Reconciliation", "Verify ledger invariants without modifying source") { onNavigateToReconciliation(snapshot.vendorId) }
            VendorHubNavCard("Cryptographic Provenance Trace", "SHA-256 fingerprints to canonical work orders") { onNavigateToProvenance(snapshot.vendorId) }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cardNavy)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No Vendor Analytics Available", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRecalculate) {
                        Text("Calculate Vendor Profitability")
                    }
                }
            }
        }
    }
}

@Composable
private fun VendorKpiItem(label: String, value: String, color: Color) {
    Column {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp)
        Text(text = value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VendorHubNavCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Text(text = "→", color = Color(0xFF06B6D4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
