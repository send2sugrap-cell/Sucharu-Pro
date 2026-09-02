package com.sucharu.sucharupro.ui.features.profitability.customer

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
import com.sucharu.sucharupro.data.api.model.profitability.CustomerProfitabilitySnapshotDto
import java.math.BigDecimal

/**
 * 1. Customer Profitability Hub Screen
 */
@Composable
fun CustomerProfitabilityHubScreen(
    snapshot: CustomerProfitabilitySnapshotDto?,
    onNavigateToDetails: (String) -> Unit = {},
    onNavigateToCostBreakdown: (String) -> Unit = {},
    onNavigateToTrend: (String) -> Unit = {},
    onNavigateToOrders: (String) -> Unit = {},
    onNavigateToJobs: (String) -> Unit = {},
    onNavigateToProducts: (String) -> Unit = {},
    onNavigateToRanking: () -> Unit = {},
    onNavigateToConcentration: () -> Unit = {},
    onNavigateToComparison: () -> Unit = {},
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgNavy)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Customer Profitability & Contribution Hub",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Module 16 Step 04 — Analytics, Contribution, Ranking & Concentration",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (snapshot == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No active customer snapshot selected", color = Color(0xFF94A3B8), fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRecalculate, colors = ButtonDefaults.buttonColors(containerColor = accentCyan)) {
                        Text("Calculate Customer Profitability", color = Color.White)
                    }
                }
            }
        } else {
            // Customer Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardNavy),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = snapshot.customerName ?: "Customer ${snapshot.customerId}",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Code: ${snapshot.customerCode ?: "N/A"} • Period: ${snapshot.periodType}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        val classColor = when (snapshot.profitabilityClassification) {
                            "HIGHLY_PROFITABLE", "PROFITABLE" -> accentEmerald
                            "LOW_MARGIN" -> accentAmber
                            "LOSS_MAKING" -> accentRose
                            else -> Color(0xFF94A3B8)
                        }
                        Surface(
                            color = classColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = snapshot.profitabilityClassification,
                                color = classColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Key Metrics Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Revenue",
                    value = "৳ ${snapshot.recognizedRevenue.toPlainString()}",
                    color = accentCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Cost",
                    value = "৳ ${snapshot.totalActualCost.toPlainString()}",
                    color = accentAmber,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Gross Profit",
                    value = "৳ ${snapshot.grossProfit.toPlainString()}",
                    color = if (snapshot.grossProfit >= BigDecimal.ZERO) accentEmerald else accentRose,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Gross Margin",
                    value = snapshot.grossMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A",
                    color = if ((snapshot.grossMarginPercentage ?: BigDecimal.ZERO) >= BigDecimal.ZERO) accentEmerald else accentRose,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = "Contribution",
                    value = "৳ ${snapshot.contributionAmount.toPlainString()}",
                    color = accentCyan,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Contrib Margin",
                    value = snapshot.contributionMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A",
                    color = accentCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            // Navigation Actions
            Text(
                text = "Analytical Deep-Dive & Explorations",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp)
            )

            NavigationActionCard("1. Customer Profitability Details", "View comprehensive margins, variables, and integrity status") {
                onNavigateToDetails(snapshot.customerId)
            }
            NavigationActionCard("2. 12 Cost Components Breakdown", "View material, labour, machine, and indirect costs") {
                onNavigateToCostBreakdown(snapshot.customerId)
            }
            NavigationActionCard("3. Order-Level Attribution", "View revenue, cost, and margin per customer order") {
                onNavigateToOrders(snapshot.customerId)
            }
            NavigationActionCard("4. Job-Level Attribution", "View production actual job costs for customer") {
                onNavigateToJobs(snapshot.customerId)
            }
            NavigationActionCard("5. Product Contribution Analysis", "View products purchased and their margin share") {
                onNavigateToProducts(snapshot.customerId)
            }
            NavigationActionCard("6. Profitability Trend", "View historical margin direction and stability") {
                onNavigateToTrend(snapshot.customerId)
            }
            NavigationActionCard("7. Customer Rankings", "Rank customers by revenue, profit, margin, and volume") {
                onNavigateToRanking()
            }
            NavigationActionCard("8. Concentration & Risk Intelligence", "Top 1/5/10 revenue share and dependency indicators") {
                onNavigateToConcentration()
            }
            NavigationActionCard("9. Customer Comparison Engine", "Compare multi-customer profitability side by side") {
                onNavigateToComparison()
            }
            NavigationActionCard("10. Non-Mutating Reconciliation", "Verify revenue/cost mathematical identities") {
                onNavigateToReconciliation(snapshot.customerId)
            }
            NavigationActionCard("11. Provenance & Hash Trace", "Trace SHA-256 fingerprints to canonical sources") {
                onNavigateToProvenance(snapshot.customerId)
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 12.sp)
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun NavigationActionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text("→", color = Color(0xFF06B6D4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
