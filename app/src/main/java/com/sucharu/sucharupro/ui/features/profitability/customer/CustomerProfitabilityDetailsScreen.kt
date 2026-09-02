package com.sucharu.sucharupro.ui.features.profitability.customer

import androidx.compose.foundation.background
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
 * 2. Customer Profitability Details Screen
 */
@Composable
fun CustomerProfitabilityDetailsScreen(
    snapshot: CustomerProfitabilitySnapshotDto,
    onBack: () -> Unit = {}
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = cardNavy),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("← Back", color = Color.White, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Customer Profitability Details",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Identification Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Customer & Period Metadata", color = accentCyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Customer Name", snapshot.customerName ?: "N/A")
                DetailRow("Customer ID", snapshot.customerId)
                DetailRow("Customer Code", snapshot.customerCode ?: "N/A")
                DetailRow("Period Type", snapshot.periodType)
                DetailRow("Calculation Version", snapshot.calculationVersion)
                DetailRow("Integrity Status", snapshot.sourceIntegrityStatus)
            }
        }

        // Financial & Margins Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Financial Performance & Margins", color = accentEmerald, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Recognized Revenue", "৳ ${snapshot.recognizedRevenue.toPlainString()}")
                DetailRow("Total Actual Cost", "৳ ${snapshot.totalActualCost.toPlainString()}")
                DetailRow("Gross Profit", "৳ ${snapshot.grossProfit.toPlainString()}", if (snapshot.grossProfit >= BigDecimal.ZERO) accentEmerald else accentRose)
                DetailRow("Gross Margin %", snapshot.grossMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A", if ((snapshot.grossMarginPercentage ?: BigDecimal.ZERO) >= BigDecimal.ZERO) accentEmerald else accentRose)
                DetailRow("Cost to Revenue %", snapshot.costToRevenuePercentage?.let { "${it.toPlainString()}%" } ?: "N/A")
                DetailRow("Classification", snapshot.profitabilityClassification)
                DetailRow("Trend", snapshot.trend)
            }
        }

        // Contribution Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Contribution Analysis", color = accentAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Attributable Variable Cost", "৳ ${snapshot.attributableVariableCost.toPlainString()}")
                DetailRow("Attributable Fixed Cost", "৳ ${snapshot.attributableFixedCost.toPlainString()}")
                DetailRow("Contribution Amount", "৳ ${snapshot.contributionAmount.toPlainString()}", accentCyan)
                DetailRow("Contribution Margin %", snapshot.contributionMarginPercentage?.let { "${it.toPlainString()}%" } ?: "N/A", accentCyan)
            }
        }

        // Volumetric & Unit Economics Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Operational Volumetrics & Unit Economics", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Order Count", snapshot.orderCount.toString())
                DetailRow("Job Count", snapshot.jobCount.toString())
                DetailRow("Product Count", snapshot.productCount.toString())
                DetailRow("Total Quantity Sold", "${snapshot.totalQuantitySold} units")
                DetailRow("Average Order Value (AOV)", snapshot.averageOrderValue?.let { "৳ ${it.toPlainString()}" } ?: "N/A")
                DetailRow("Average Job Value (AJV)", snapshot.averageJobValue?.let { "৳ ${it.toPlainString()}" } ?: "N/A")
                DetailRow("Avg Revenue Per Unit (ARPU)", snapshot.averageRevenuePerUnit?.let { "৳ ${it.toPlainString()}" } ?: "N/A")
                DetailRow("Avg Cost Per Unit", snapshot.averageCostPerUnit?.let { "৳ ${it.toPlainString()}" } ?: "N/A")
                DetailRow("Avg Profit Per Unit", snapshot.averageProfitPerUnit?.let { "৳ ${it.toPlainString()}" } ?: "N/A")
            }
        }

        // Integrity & Audit Card
        Card(
            colors = CardDefaults.cardColors(containerColor = cardNavy),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Integrity & Tamper Evidence", color = Color(0xFF94A3B8), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow("Snapshot ID", snapshot.snapshotId)
                DetailRow("Reconciled", snapshot.isReconciled.toString(), if (snapshot.isReconciled) accentEmerald else accentRose)
                DetailRow("SHA-256 Hash", snapshot.integrityHash.take(16) + "...", Color(0xFF38BDF8))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFF94A3B8), fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
