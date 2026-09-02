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
import com.sucharu.sucharupro.data.api.model.ReviewCompletionRequestDto
import com.sucharu.sucharupro.data.api.model.SubmitCompletionRequestDto
import com.sucharu.sucharupro.data.api.model.VendorCompletionRequestDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalCompletionRequestScreen(
    workOrderId: String,
    workOrderNumber: String,
    targetQuantity: Double,
    isInternalReviewer: Boolean = false,
    completionRequest: VendorCompletionRequestDto?,
    onSubmitRequest: (SubmitCompletionRequestDto) -> Unit = {},
    onReviewRequest: (ReviewCompletionRequestDto) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var notesText by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf(targetQuantity.toString()) }
    var reviewNotesText by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Completion Request - $workOrderNumber",
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Existing Request Card
                if (completionRequest != null) {
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
                                Text("Formal Completion Sign-off", fontWeight = FontWeight.Bold, color = Color.White)
                                CompletionStatusBadge(status = completionRequest.status)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Final Completed Quantity: ${completionRequest.finalCompletedQuantity}", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Notes: ${completionRequest.completionNotes}", color = Color(0xFFCBD5E1), fontSize = 13.sp)

                            completionRequest.reviewNotes?.let { rev ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Review Outcome Notes: $rev", color = if (completionRequest.status == "APPROVED") Color(0xFF34D399) else Color(0xFFF87171), fontSize = 13.sp)
                            }
                        }
                    }

                    // Internal Review Actions
                    if (isInternalReviewer && completionRequest.status == "PENDING_REVIEW") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Internal QA / Procurement Review", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = reviewNotesText,
                                    onValueChange = { reviewNotesText = it },
                                    label = { Text("Review / Inspection Notes") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onReviewRequest(ReviewCompletionRequestDto(approved = true, reviewNotes = reviewNotesText.ifBlank { "Approved" })) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                    ) {
                                        Text("Approve & Complete WO")
                                    }
                                    Button(
                                        onClick = { onReviewRequest(ReviewCompletionRequestDto(approved = false, reviewNotes = reviewNotesText.ifBlank { "Rejected" })) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                    ) {
                                        Text("Reject")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Vendor Submission Form
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Request Work Order Sign-off", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Submit final completion for internal QA verification.", fontSize = 13.sp, color = Color(0xFF94A3B8))

                            OutlinedTextField(
                                value = qtyText,
                                onValueChange = { qtyText = it },
                                label = { Text("Final Completed Quantity") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                label = { Text("Completion Summary / Handover Notes") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val fQty = qtyText.toDoubleOrNull() ?: targetQuantity
                                    onSubmitRequest(
                                        SubmitCompletionRequestDto(
                                            completionNotes = notesText.ifBlank { "Job finished" },
                                            finalCompletedQuantity = fQty
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Submit Completion Request")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "PENDING_REVIEW" -> Color(0xFF78350F) to Color(0xFFFBBF24)
        "APPROVED" -> Color(0xFF064E3B) to Color(0xFF10B981)
        "REJECTED" -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        else -> Color(0xFF1E293B) to Color(0xFF94A3B8)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
