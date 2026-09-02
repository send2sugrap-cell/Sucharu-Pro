package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
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
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialDisputeDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalFinancialDisputeDetailsScreen(
    dispute: VendorPortalFinancialDisputeDto,
    onRespondClick: (String) -> Unit = {},
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
                    Text(dispute.disputeNumber, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
            // Main Card
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
                                text = "Dispute: ${dispute.category}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            StatusBadge(status = dispute.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(14.dp))

                        SettlementDetailRow("Disputed Amount", "${String.format("%,.2f", dispute.disputedAmount)} ${dispute.currency}")
                        dispute.proposedResolutionAmount?.let {
                            SettlementDetailRow("Proposed Resolution", "${String.format("%,.2f", it)} ${dispute.currency}")
                        }
                        SettlementDetailRow("Priority", dispute.priority)
                        dispute.settlementId?.let { SettlementDetailRow("Settlement ID", it) }
                        dispute.invoiceId?.let { SettlementDetailRow("Invoice ID", it) }
                        SettlementDetailRow("Filed At", dateFormat.format(Date(dispute.createdAt)))

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Dispute Reason:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                        Text(text = dispute.reason, fontSize = 13.sp, color = Color(0xFFCBD5E1))

                        dispute.resolutionNotes?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Resolution Notes:", fontSize = 12.sp, color = Color(0xFF34D399))
                            Text(text = it, fontSize = 13.sp, color = Color.White)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onRespondClick(dispute.disputeId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Post Dispute Response / Update")
                        }
                    }
                }
            }

            // Timeline Header
            item {
                Text(
                    text = "Dispute Events & Notes (${dispute.events.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            items(dispute.events) { event ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${event.action} (${event.actorRole})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFF87171)
                            )
                            Text(
                                text = dateFormat.format(Date(event.timestamp)),
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = event.remarks, fontSize = 13.sp, color = Color(0xFFE2E8F0))
                    }
                }
            }
        }
    }
}
