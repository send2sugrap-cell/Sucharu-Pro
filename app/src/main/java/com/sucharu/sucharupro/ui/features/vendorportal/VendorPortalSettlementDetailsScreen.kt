package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.data.api.model.VendorPortalSettlementSummaryDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalSettlementDetailsScreen(
    settlement: VendorPortalSettlementSummaryDto,
    onViewAllocationsClick: (String) -> Unit = {},
    onAcknowledgeClick: (String) -> Unit = {},
    onRaiseDisputeClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(settlement.settlementNumber, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            // Header card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Settlement Overview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            StatusBadge(status = settlement.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Net Amount Payable",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${String.format("%,.2f", settlement.netPayable)} ${settlement.currency}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettlementDetailRow("Settlement Date", SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(settlement.settlementDate)))
                        SettlementDetailRow("Payment Method", settlement.settlementMethod)
                        settlement.maskedPaymentReference?.let {
                            SettlementDetailRow("Payment Reference", it)
                        }
                        SettlementDetailRow("Allocations Count", "${settlement.allocationCount}")
                        SettlementDetailRow("Acknowledgement", settlement.acknowledgementStatus)
                        settlement.approvedAt?.let {
                            SettlementDetailRow("Approved At", dateFormat.format(Date(it)))
                        }
                        settlement.settledAt?.let {
                            SettlementDetailRow("Settled At", dateFormat.format(Date(it)))
                        }
                        settlement.notes?.let {
                            SettlementDetailRow("Notes", it)
                        }
                    }
                }
            }

            // Actions card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Collaboration Actions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onViewAllocationsClick(settlement.settlementId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Allocation Breakdown")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onAcknowledgeClick(settlement.settlementId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Acknowledge Settlement")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { onRaiseDisputeClick(settlement.settlementId) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                        ) {
                            Text("Raise Dispute on Settlement")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettlementDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 13.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    }
}
