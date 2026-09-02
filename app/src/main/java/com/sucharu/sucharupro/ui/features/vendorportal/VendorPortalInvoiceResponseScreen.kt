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
import com.sucharu.sucharupro.data.api.model.RespondInvoiceRequestDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalInvoiceResponseScreen(
    invoiceId: String,
    onSubmitResponse: (RespondInvoiceRequestDto) -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    var responseType by remember { mutableStateOf("CLARIFY_EXCEPTION") }
    var comment by remember { mutableStateOf("") }
    var proposedCorrection by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Invoice Clarification / Dispute",
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
            Text("Response Type", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

            val types = listOf(
                "CLARIFY_EXCEPTION" to "Clarification",
                "ACCEPT_VARIANCE" to "Accept Variance",
                "DISPUTE_VARIANCE" to "Dispute Variance",
                "PROPOSE_CORRECTION" to "Propose Correction",
                "SUBMIT_ADDITIONAL_DOCS" to "Additional Evidence"
            )

            types.forEach { (typeKey, typeLabel) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    RadioButton(
                        selected = responseType == typeKey,
                        onClick = { responseType = typeKey }
                    )
                    Text(
                        text = typeLabel,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Response / Explanation") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color.Gray
                )
            )

            if (responseType == "PROPOSE_CORRECTION") {
                OutlinedTextField(
                    value = proposedCorrection,
                    onValueChange = { proposedCorrection = it },
                    label = { Text("Proposed Corrective Amount / Details") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }

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
                        onSubmitResponse(
                            RespondInvoiceRequestDto(
                                responseType = responseType,
                                comment = comment,
                                proposedCorrection = proposedCorrection.ifBlank { null }
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp),
                    enabled = comment.isNotBlank()
                ) {
                    Text("Submit Response", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
