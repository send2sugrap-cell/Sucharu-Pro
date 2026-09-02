package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialKpiSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalFinancialSummaryScreen(
    kpi: VendorPortalFinancialKpiSummaryDto,
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
                        text = "Financial KPI Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Total Invoiced",
                    value = "${kpi.currency} ${String.format("%.2f", kpi.totalInvoiced)}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF38BDF8)
                )
                KpiMetricCard(
                    title = "Total Approved",
                    value = "${kpi.currency} ${String.format("%.2f", kpi.totalApproved)}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF4ADE80)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Total Paid",
                    value = "${kpi.currency} ${String.format("%.2f", kpi.totalPaid)}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF22C55E)
                )
                KpiMetricCard(
                    title = "Outstanding",
                    value = "${kpi.currency} ${String.format("%.2f", kpi.totalOutstanding)}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFBBF24)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Total Disputed",
                    value = "${kpi.currency} ${String.format("%.2f", kpi.totalDisputed)}",
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF87171)
                )
                KpiMetricCard(
                    title = "Invoices Count",
                    value = "${kpi.invoiceCount} (${kpi.paidInvoiceCount} Paid)",
                    modifier = Modifier.weight(1f),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
