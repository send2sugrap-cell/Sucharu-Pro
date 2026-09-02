package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalSettlementAnalyticsSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalFinancialAnalyticsScreen(
    analytics: VendorPortalSettlementAnalyticsSummaryDto,
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Vendor Financial Analytics", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Financial Volume & Balances",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettlementDetailRow("Total Settled", "${String.format("%,.2f", analytics.totalSettledAmount)} ${analytics.currency}")
                        SettlementDetailRow("Total Outstanding Payables", "${String.format("%,.2f", analytics.totalOutstandingAmount)} ${analytics.currency}")
                        SettlementDetailRow("Total Disputed Amount", "${String.format("%,.2f", analytics.totalDisputedAmount)} ${analytics.currency}")
                        SettlementDetailRow("Total Reconciled Amount", "${String.format("%,.2f", analytics.totalReconciledAmount)} ${analytics.currency}")
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Operational Efficiency & Dispute Rates",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettlementDetailRow("Average Settlement Cycle", "${String.format("%.1f", analytics.averageSettlementCycleDays)} Days")
                        SettlementDetailRow("Dispute Resolution Rate", "${String.format("%.1f", analytics.disputeResolutionRate)}%")
                        SettlementDetailRow("Active Dispute Cases", "${analytics.activeDisputeCount}")
                        SettlementDetailRow("Pending Reconciliation Inquiries", "${analytics.pendingReconciliationCount}")
                    }
                }
            }
        }
    }
}
