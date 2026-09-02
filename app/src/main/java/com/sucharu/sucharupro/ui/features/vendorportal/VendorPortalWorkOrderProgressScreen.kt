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
import com.sucharu.sucharupro.data.api.model.SubmitProgressRequestDto
import com.sucharu.sucharupro.data.api.model.VendorProgressUpdateDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalWorkOrderProgressScreen(
    workOrderId: String,
    workOrderNumber: String,
    authorizedQuantity: Double,
    unitOfMeasure: String,
    progressUpdates: List<VendorProgressUpdateDto>,
    onSubmitProgress: (SubmitProgressRequestDto) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var completedQtyText by remember { mutableStateOf("") }
    var remainingQtyText by remember { mutableStateOf("") }
    var statusSummaryText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Progress Updates - $workOrderNumber",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSubmitDialog = true },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White
            ) {
                Text("+ Submit Progress", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Authorized Scope", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("$authorizedQuantity $unitOfMeasure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        val latest = progressUpdates.firstOrNull()
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Latest Progress", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Text("${latest?.progressPercentage?.toInt() ?: 0}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Progress History (${progressUpdates.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))

                if (progressUpdates.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No progress updates recorded yet.", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(progressUpdates) { update ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(update.statusSummary, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${update.progressPercentage?.toInt() ?: 0}%", fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Completed: ${update.completedQuantity} | Remaining: ${update.remainingQuantity}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    update.notes?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = it, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Submit Progress Update") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = completedQtyText,
                        onValueChange = { completedQtyText = it },
                        label = { Text("Completed Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = remainingQtyText,
                        onValueChange = { remainingQtyText = it },
                        label = { Text("Remaining Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = statusSummaryText,
                        onValueChange = { statusSummaryText = it },
                        label = { Text("Status Summary (e.g. 50% cutting completed)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Detailed Notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cQty = completedQtyText.toDoubleOrNull() ?: 0.0
                        val rQty = remainingQtyText.toDoubleOrNull() ?: (authorizedQuantity - cQty)
                        val pct = if (authorizedQuantity > 0) (cQty / authorizedQuantity) * 100.0 else 0.0
                        onSubmitProgress(
                            SubmitProgressRequestDto(
                                completedQuantity = cQty,
                                remainingQuantity = rQty,
                                progressPercentage = pct,
                                statusSummary = statusSummaryText.ifBlank { "Progress update" },
                                notes = notesText.ifBlank { null }
                            )
                        )
                        showSubmitDialog = false
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("Cancel") }
            }
        )
    }
}
