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
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialDisputeCreateRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalFinancialDisputeCreateScreen(
    prefilledSettlementId: String? = null,
    prefilledInvoiceId: String? = null,
    onSubmitDispute: (VendorPortalFinancialDisputeCreateRequest) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var settlementId by remember { mutableStateOf(prefilledSettlementId ?: "") }
    var invoiceId by remember { mutableStateOf(prefilledInvoiceId ?: "") }
    var category by remember { mutableStateOf("PAYMENT_CALCULATION") }
    var priority by remember { mutableStateOf("NORMAL") }
    var disputedAmountText by remember { mutableStateOf("") }
    var proposedAmountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("File Financial Dispute", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Cancel", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Dispute Classification & Context",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = settlementId,
                        onValueChange = { settlementId = it },
                        label = { Text("Settlement ID (Optional)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = invoiceId,
                        onValueChange = { invoiceId = it },
                        label = { Text("Invoice ID (Optional)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Dispute Category (e.g. DEDUCTION, RATE, MISSING_ALLOCATION)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = disputedAmountText,
                        onValueChange = { disputedAmountText = it },
                        label = { Text("Disputed Amount (BDT)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = proposedAmountText,
                        onValueChange = { proposedAmountText = it },
                        label = { Text("Proposed Resolution Amount (Optional)", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Detailed Reason & Justification", color = Color(0xFF94A3B8)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val amount = disputedAmountText.toDoubleOrNull() ?: 0.0
                            val propAmount = proposedAmountText.toDoubleOrNull()
                            if (amount > 0 && reason.isNotBlank()) {
                                onSubmitDispute(
                                    VendorPortalFinancialDisputeCreateRequest(
                                        settlementId = settlementId.ifBlank { null },
                                        invoiceId = invoiceId.ifBlank { null },
                                        category = category,
                                        priority = priority,
                                        disputedAmount = amount,
                                        proposedResolutionAmount = propAmount,
                                        currency = "BDT",
                                        reason = reason
                                    )
                                )
                            }
                        },
                        enabled = (disputedAmountText.toDoubleOrNull() ?: 0.0) > 0 && reason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Submit Financial Dispute", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
