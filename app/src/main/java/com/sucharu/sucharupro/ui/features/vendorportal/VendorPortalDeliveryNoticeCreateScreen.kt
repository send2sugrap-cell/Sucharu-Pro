package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.CreateDeliveryNoticeItemRequestDto
import com.sucharu.sucharupro.data.api.model.CreateDeliveryNoticeRequestDto
import com.sucharu.sucharupro.data.api.model.VendorPortalPurchaseOrderSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDeliveryNoticeCreateScreen(
    eligiblePurchaseOrders: List<VendorPortalPurchaseOrderSummaryDto>,
    onSubmit: (CreateDeliveryNoticeRequestDto) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    var selectedPoId by remember { mutableStateOf(eligiblePurchaseOrders.firstOrNull()?.purchaseOrderId ?: "") }
    var carrierName by remember { mutableStateOf("") }
    var trackingNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf("") }
    var vendorNotes by remember { mutableStateOf("") }
    var itemQtyText by remember { mutableStateOf("100") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Advance Shipping Notice (ASN)",
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        OutlinedTextField(
                            value = selectedPoId,
                            onValueChange = { selectedPoId = it },
                            label = { Text("Purchase Order ID") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = carrierName,
                            onValueChange = { carrierName = it },
                            label = { Text("Carrier / Logistics Partner") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = trackingNumber,
                            onValueChange = { trackingNumber = it },
                            label = { Text("Tracking Number") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = vehicleNumber,
                                onValueChange = { vehicleNumber = it },
                                label = { Text("Vehicle No.") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = itemQtyText,
                                onValueChange = { itemQtyText = it },
                                label = { Text("Dispatch Qty") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = driverName,
                                onValueChange = { driverName = it },
                                label = { Text("Driver Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = driverPhone,
                                onValueChange = { driverPhone = it },
                                label = { Text("Driver Phone") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = vendorNotes,
                            onValueChange = { vendorNotes = it },
                            label = { Text("Dispatch Remarks / Instructions") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = {
                            val qty = itemQtyText.toDoubleOrNull() ?: 1.0
                            val req = CreateDeliveryNoticeRequestDto(
                                purchaseOrderId = selectedPoId,
                                plannedDeliveryDate = System.currentTimeMillis() + 86400000L,
                                carrierName = carrierName.takeIf { it.isNotBlank() },
                                trackingNumber = trackingNumber.takeIf { it.isNotBlank() },
                                vehicleNumber = vehicleNumber.takeIf { it.isNotBlank() },
                                driverName = driverName.takeIf { it.isNotBlank() },
                                driverPhone = driverPhone.takeIf { it.isNotBlank() },
                                vendorNotes = vendorNotes.takeIf { it.isNotBlank() },
                                items = listOf(
                                    CreateDeliveryNoticeItemRequestDto(
                                        purchaseOrderItemId = "poi-default-1",
                                        deliveryQuantity = qty
                                    )
                                )
                            )
                            onSubmit(req)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create ASN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
