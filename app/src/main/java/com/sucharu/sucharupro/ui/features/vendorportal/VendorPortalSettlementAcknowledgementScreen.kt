package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.sucharu.sucharupro.data.api.model.VendorPortalSettlementAcknowledgementRequest
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalSettlementAcknowledgementScreen(
    settlementId: String,
    settlementNumber: String,
    netPayableFormatted: String,
    onSubmitAcknowledgement: (VendorPortalSettlementAcknowledgementRequest) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var discrepancyFlag by remember { mutableStateOf(false) }
    var discrepancyNotes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Acknowledge Settlement", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Settlement: $settlementNumber",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Net Amount: $netPayableFormatted",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Flag Discrepancy",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Check this if you disagree with the calculated net amount or deductions.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = discrepancyFlag,
                            onCheckedChange = { discrepancyFlag = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFEF4444),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                    }

                    if (discrepancyFlag) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = discrepancyNotes,
                            onValueChange = { discrepancyNotes = it },
                            label = { Text("Discrepancy Details / Explanation", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFEF4444),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (discrepancyFlag) Color(0xFFF59E0B) else Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (discrepancyFlag) "Submit Acknowledgement with Discrepancy" else "Confirm & Acknowledge Settlement",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Acknowledgement", color = Color.White) },
            text = {
                Text(
                    text = if (discrepancyFlag)
                        "You are acknowledging settlement $settlementNumber with an attached discrepancy note."
                    else
                        "You are confirming receipt and acceptance of settlement $settlementNumber for $netPayableFormatted.",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSubmitAcknowledgement(
                            VendorPortalSettlementAcknowledgementRequest(
                                status = if (discrepancyFlag) "ACKNOWLEDGED_WITH_DISCREPANCY" else "ACKNOWLEDGED",
                                idempotencyKey = "ACK-${settlementId}-${UUID.randomUUID().toString().take(8)}",
                                discrepancyFlag = discrepancyFlag,
                                discrepancyNotes = if (discrepancyFlag) discrepancyNotes else null
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
