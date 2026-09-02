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
import com.sucharu.sucharupro.data.api.model.SubmitVendorInvoiceItemRequestDto
import com.sucharu.sucharupro.data.api.model.SubmitVendorInvoiceRequestDto
import com.sucharu.sucharupro.data.api.model.VendorPurchaseOrderDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalInvoiceCreateScreen(
    purchaseOrders: List<VendorPurchaseOrderDto>,
    selectedPoId: String? = null,
    onSubmitClick: (SubmitVendorInvoiceRequestDto) -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    var chosenPoId by remember { mutableStateOf(selectedPoId ?: purchaseOrders.firstOrNull()?.purchaseOrderId ?: "") }
    var vendorInvoiceNumber by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BDT") }
    var shippingAmount by remember { mutableStateOf("0.0") }
    var otherCharges by remember { mutableStateOf("0.0") }
    var notes by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    val currentPo = purchaseOrders.firstOrNull { it.purchaseOrderId == chosenPoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Submit Commercial Invoice",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = vendorInvoiceNumber,
                onValueChange = { vendorInvoiceNumber = it },
                label = { Text("Vendor Invoice Number / Tax Ref") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.Gray
                )
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Payment Instructions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.Gray
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = Color.LightGray)
                }

                Button(
                    onClick = {
                        val items = currentPo?.items?.map { itm ->
                            SubmitVendorInvoiceItemRequestDto(
                                purchaseOrderItemId = itm.itemId ?: "",
                                invoicedQuantity = itm.quantity,
                                unitPrice = itm.unitRate
                            )
                        } ?: emptyList()

                        onSubmitClick(
                            SubmitVendorInvoiceRequestDto(
                                purchaseOrderId = chosenPoId,
                                vendorInvoiceNumber = vendorInvoiceNumber,
                                currency = currency,
                                shippingAmount = shippingAmount.toDoubleOrNull(),
                                otherCharges = otherCharges.toDoubleOrNull(),
                                notes = notes.ifBlank { null },
                                items = items
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = vendorInvoiceNumber.isNotBlank() && chosenPoId.isNotBlank()
                ) {
                    Text("Submit Draft", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
