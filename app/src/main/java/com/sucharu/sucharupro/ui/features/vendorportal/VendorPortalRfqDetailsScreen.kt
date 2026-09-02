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
import com.sucharu.sucharupro.data.api.model.VendorRfqDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalRfqDetailsScreen(
    rfq: VendorRfqDto,
    onAcknowledgeClick: () -> Unit = {},
    onDeclineClick: () -> Unit = {},
    onCreateQuotationClick: () -> Unit = {},
    onAskClarificationClick: () -> Unit = {},
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
                        text = rfq.rfqNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rfq.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                RfqStatusBadge(status = rfq.status)
                            }
                            val description = rfq.description
                            if (!description.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = description,
                                    fontSize = 14.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Deadline", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    Text("${rfq.responseDeadline}", fontSize = 13.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Currency", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    Text(rfq.currency, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Payment Terms", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    Text(rfq.paymentTerms ?: "Standard", fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Action buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCreateQuotationClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                        ) {
                            Text("Prepare Bid")
                        }
                        OutlinedButton(
                            onClick = onAskClarificationClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clarify", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onAcknowledgeClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Acknowledge", color = Color(0xFF34D399))
                        }
                    }
                }

                // Items list header
                item {
                    Text(
                        text = "Requested Requirements (${rfq.items.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Line items
                items(rfq.items) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "#${item.sequenceNumber} - ${item.description}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${item.quantity} ${item.unitOfMeasure}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA)
                                )
                            }
                            if (item.specifications != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Specs: ${item.specifications}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
