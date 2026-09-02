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
import com.sucharu.sucharupro.data.api.model.VendorPortalWorkOrderDetailsDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalWorkOrderDetailsScreen(
    wo: VendorPortalWorkOrderDetailsDto?,
    onAcknowledgeClick: (String, String?) -> Unit = { _, _ -> },
    onDeclineClick: (String) -> Unit = {},
    onSubmitProgressClick: () -> Unit = {},
    onReportBlockerClick: () -> Unit = {},
    onRegisterEvidenceClick: () -> Unit = {},
    onRequestCompletionClick: () -> Unit = {},
    onOpenThreadClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showAckDialog by remember { mutableStateOf(false) }
    var showDeclineDialog by remember { mutableStateOf(false) }
    var ackComment by remember { mutableStateOf("") }
    var declineReason by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = wo?.workOrderNumber ?: "Work Order Details",
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
            if (wo == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = wo.workOrderNumber,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF60A5FA)
                                    )
                                    WorkOrderStatusBadge(status = wo.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = wo.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                val description = wo.description
                                if (!description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = description,
                                        fontSize = 13.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Quantity Required", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("${wo.quantity} ${wo.unitOfMeasure}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Column {
                                        Text("Estimated Amount", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("${wo.currency} ${wo.estimatedAmount}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                }
                            }
                        }
                    }

                    // Acknowledgement Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Vendor Job Acknowledgement", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(modifier = Modifier.height(8.dp))
                                val ack = wo.acknowledgement
                                if (ack != null) {
                                    Text("Outcome: ${ack.acknowledgementType}", color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                                    ack.comment?.let {
                                        Text("Comment: $it", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    }
                                } else {
                                    Text("Pending vendor formal job acknowledgement.", color = Color(0xFFFBBF24), fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { showAckDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("Acknowledge Job")
                                        }
                                        OutlinedButton(
                                            onClick = { showDeclineDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171))
                                        ) {
                                            Text("Decline")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action Grid
                    item {
                        Text("Operational Collaboration", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onSubmitProgressClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Progress (${wo.progressUpdates.size})")
                            }
                            Button(
                                onClick = onReportBlockerClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (wo.blockers.any { it.status == "OPEN" }) Color(0xFFDC2626) else Color(0xFFD97706))
                            ) {
                                Text("Blockers (${wo.blockers.size})")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onRegisterEvidenceClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                            ) {
                                Text("Evidence (${wo.evidenceList.size})")
                            }
                            Button(
                                onClick = onRequestCompletionClick,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Text(if (wo.completionRequest != null) "Review Request" else "Request Completion")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onOpenThreadClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                        ) {
                            Text("Open Collaboration Thread")
                        }
                    }

                    // Recent Progress Summary
                    if (wo.progressUpdates.isNotEmpty()) {
                        item {
                            Text("Recent Progress Updates", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        items(wo.progressUpdates.take(3)) { prog ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(prog.statusSummary, fontWeight = FontWeight.Medium, color = Color.White)
                                        Text("${prog.completedQuantity} / ${prog.authorizedQuantity}", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                    }
                                    prog.notes?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, fontSize = 12.sp, color = Color(0xFF94A3B8))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAckDialog) {
        AlertDialog(
            onDismissRequest = { showAckDialog = false },
            title = { Text("Acknowledge Work Order") },
            text = {
                Column {
                    Text("Confirm acceptance of Work Order ${wo?.workOrderNumber}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = ackComment,
                        onValueChange = { ackComment = it },
                        label = { Text("Comment (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showAckDialog = false
                    onAcknowledgeClick("ACKNOWLEDGED", ackComment.ifBlank { null })
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAckDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeclineDialog) {
        AlertDialog(
            onDismissRequest = { showDeclineDialog = false },
            title = { Text("Decline Work Order") },
            text = {
                Column {
                    Text("Please provide the reason for declining:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        label = { Text("Decline Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeclineDialog = false
                        onDeclineClick(declineReason)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Decline WO")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclineDialog = false }) { Text("Cancel") }
            }
        )
    }
}
