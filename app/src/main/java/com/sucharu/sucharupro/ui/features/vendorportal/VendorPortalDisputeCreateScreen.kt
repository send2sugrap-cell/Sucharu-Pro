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
import com.sucharu.sucharupro.data.api.model.VendorPortalDisputeCreateRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDisputeCreateScreen(
    sourceType: String = "REJECTION",
    sourceId: String = "",
    onSubmit: (VendorPortalDisputeCreateRequest) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var disputeType by remember { mutableStateOf("QUALITY") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var requestedResolution by remember { mutableStateOf("REPLACEMENT") }
    var disputedQuantity by remember { mutableStateOf("0") }
    var disputedAmount by remember { mutableStateOf("0") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Raise Quality / Financial Dispute",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dispute Information", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject / Summary") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detailed Dispute Justification") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )

                    OutlinedTextField(
                        value = disputedQuantity,
                        onValueChange = { disputedQuantity = it },
                        label = { Text("Disputed Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = disputedAmount,
                        onValueChange = { disputedAmount = it },
                        label = { Text("Disputed Amount ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }

                Button(
                    onClick = {
                        val qty = disputedQuantity.toDoubleOrNull() ?: 0.0
                        val amt = disputedAmount.toDoubleOrNull() ?: 0.0
                        onSubmit(
                            VendorPortalDisputeCreateRequest(
                                sourceType = sourceType,
                                sourceId = sourceId,
                                disputeType = disputeType,
                                priority = priority,
                                subject = subject,
                                description = description,
                                requestedResolution = requestedResolution,
                                disputedQuantity = qty,
                                disputedAmount = amt
                            )
                        )
                    },
                    enabled = subject.isNotBlank() && description.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Submit Dispute", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
