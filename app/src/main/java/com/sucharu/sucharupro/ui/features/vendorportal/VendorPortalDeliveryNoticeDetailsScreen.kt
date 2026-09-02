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
import com.sucharu.sucharupro.data.api.model.VendorPortalDeliveryNoticeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDeliveryNoticeDetailsScreen(
    notice: VendorPortalDeliveryNoticeDto,
    onSubmitNoticeClick: () -> Unit = {},
    onCancelNoticeClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var cancelReason by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "ASN: ${notice.noticeNumber}",
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status: ${notice.status}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            Text("PO: ${notice.orderNumber}", color = Color(0xFFE2E8F0))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (notice.carrierName != null) {
                            Text("Carrier: ${notice.carrierName}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                        if (notice.trackingNumber != null) {
                            Text("Tracking: ${notice.trackingNumber}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                        if (notice.vehicleNumber != null) {
                            Text("Vehicle: ${notice.vehicleNumber}", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                        if (notice.driverName != null) {
                            Text("Driver: ${notice.driverName} (${notice.driverPhone ?: "N/A"})", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dispatch Items", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notice.items) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(item.itemName, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Ordered: ${item.orderedQuantity} ${item.unitOfMeasure}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                                Text("Delivering: ${item.deliveryQuantity}", color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action buttons if DRAFT
                if (notice.status == "DRAFT") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Cancel ASN", color = Color(0xFFF87171))
                        }
                        Button(
                            onClick = onSubmitNoticeClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Submit ASN", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showCancelDialog) {
                AlertDialog(
                    onDismissRequest = { showCancelDialog = false },
                    title = { Text("Cancel Delivery Notice") },
                    text = {
                        OutlinedTextField(
                            value = cancelReason,
                            onValueChange = { cancelReason = it },
                            label = { Text("Reason for cancellation") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            showCancelDialog = false
                            onCancelNoticeClick(cancelReason)
                        }) {
                            Text("Confirm Cancel")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCancelDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }
        }
    }
}
