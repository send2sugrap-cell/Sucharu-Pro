package com.sucharu.sucharupro.ui.features.communication.vendor.document

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgColor = Color(0xFF0F172A)
private val SurfaceColor = Color(0xFF1E293B)
private val AccentColor = Color(0xFF38BDF8)
private val AccentGreen = Color(0xFF22D3EE)
private val AccentRed = Color(0xFFF87171)
private val TextPrimary = Color(0xFFF1F5F9)
private val TextSecondary = Color(0xFF94A3B8)
private val InputBg = Color(0xFF334155)

@Composable
fun VendorDocumentReviewScreen(
    documentId: String,
    viewModel: VendorDocumentReviewViewModel,
    onNavigateBack: () -> Unit,
    onReviewComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onReviewComplete()
    }

    Column(modifier = Modifier.fillMaxSize().background(BgColor)) {
        VendorDocTopBar(title = "Review Document", onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val doc = state.document
            if (doc != null) {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SurfaceColor)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(doc.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(doc.documentType.defaultLabel, color = AccentColor, fontSize = 12.sp)
                        Text("v${doc.documentVersion} • ${doc.documentNo}", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            Text("Review Decision", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = state.remarks,
                onValueChange = viewModel::updateRemarks,
                label = { Text("Remarks", color = TextSecondary, fontSize = 12.sp) },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentColor,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentColor,
                    cursorColor = AccentColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.rejectionReason,
                onValueChange = viewModel::updateRejectionReason,
                label = { Text("Rejection Reason (if rejecting)", color = TextSecondary, fontSize = 12.sp) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentRed,
                    unfocusedBorderColor = InputBg,
                    focusedLabelColor = AccentRed,
                    cursorColor = AccentRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (state.error != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = AccentRed.copy(alpha = 0.12f)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(state.error!!, color = AccentRed, fontSize = 12.sp)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::approve,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = viewModel::reject,
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AccentRed)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reject", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
