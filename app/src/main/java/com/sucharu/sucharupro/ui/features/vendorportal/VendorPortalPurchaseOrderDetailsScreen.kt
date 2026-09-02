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
import com.sucharu.sucharupro.data.api.model.VendorPortalPurchaseOrderDetailsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalPurchaseOrderDetailsScreen(
    po: VendorPortalPurchaseOrderDetailsDto?,
    onAcknowledgeClick: (String, String?) -> Unit = { _, _ -> },
    onDeclineClick: (String) -> Unit = {},
    onViewWorkOrdersClick: () -> Unit = {},
    onOpenThreadClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showAckDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var ackComment by remember { mutableStateOf("") }
    var declineReason by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = po?.orderNumber ?: "Purchase Order Details",
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
            if (po == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
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
                                        text = po.orderNumber,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF60A5FA)
                                    )
                                    PoStatusBadge(status = po.status)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Amount", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("${po.currency} ${po.totalAmount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column {
                                        Text("Delivery Location", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text(po.deliveryLocation ?: "Standard", fontSize = 14.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    // Acknowledgement Status or Action Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Vendor Acknowledgement", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                val ack = po.acknowledgement
                                if (ack != null) {
                                    Text("Status: ${ack.acknowledgementType}", color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                                    ack.comment?.let {
                                        Text("Notes: $it", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    }
                                } else {
                                    Text("Pending vendor formal acknowledgement.", color = Color(0xFFFBBF24), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { showAckDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("Acknowledge PO")
                                        }
                                        OutlinedButton(
                                            onClick = { showDeclineDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                                        ) {
                                            Text("Decline")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // PO Line Items
                    item {
                        Text("Order Line Items (${po.items.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    items(po.items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.itemDescription, fontWeight = FontWeight.Medium, color = Color.White)
                                    Text("${item.quantity} ${item.unitOfMeasure} @ ${item.unitRate}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                }
                                Text("${item.currency} ${item.lineTotal}", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                            }
                        }
                    }

                    // Navigation Action Buttons
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onViewWorkOrdersClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Text("Work Orders")
                            }
                            Button(
                                onClick = onOpenThreadClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Collaboration")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAckDialog) {
        AlertDialog(
            onDismissRequest = { showAckDialog = false },
            title = { Text("Acknowledge Purchase Order") },
            text = {
                Column {
                    Text("Confirm acceptance of PO ${po?.orderNumber}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ackComment,
                        onValueChange = { ackComment = it },
                        label = { Text("Comment (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAckDialog = false
                    onAcknowledgeClick("ACKNOWLEDGED", ackComment.ifBlank { null })
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAckDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline Purchase Order") },
            text = {
                Column {
                    Text("Please provide the reason for declining:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        label = { Text("Decline Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeclineDialog = false
                        onDeclineClick(declineReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Decline PO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) { Text("Cancel") }
            }
        )
    }
}
