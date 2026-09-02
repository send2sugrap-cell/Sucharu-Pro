package com.sucharu.sucharupro.ui.features.finance.reconciliation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialReconciliationDiscrepancy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FinancialDiscrepancyScreen(
    viewModel: FinancialReconciliationViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.discrepanciesState.collectAsState()
    var selectedForAction by remember { mutableStateOf<FinancialReconciliationDiscrepancy?>(null) }
    var actionType by remember { mutableStateOf<String?>(null) } // "RESOLVE" or "WAIVE"
    var actionNote by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                }
                Text(
                    text = "Financial Discrepancies",
                    color = Color(0xFFF8FAFC),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.discrepancies.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.padding(bottom = 8.dp))
                                    Text("Zero open financial discrepancies detected.", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("All subsystem balances match expected controls.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        items(state.discrepancies) { disc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(disc.discrepancyNo, color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(disc.type.defaultLabel, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            DiscrepancySeverityBadge(severity = disc.severity)
                                            DiscrepancyStatusBadge(status = disc.status)
                                        }
                                    }

                                    Text(disc.description, color = Color(0xFFCBD5E1), fontSize = 12.sp)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Expected", color = Color(0xFF64748B), fontSize = 10.sp)
                                            Text(disc.expectedAmount.formatted(), color = Color(0xFFF8FAFC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column {
                                            Text("Actual", color = Color(0xFF64748B), fontSize = 10.sp)
                                            Text(disc.actualAmount.formatted(), color = Color(0xFFF8FAFC), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Difference", color = Color(0xFF64748B), fontSize = 10.sp)
                                            Text(disc.differenceAmount.formatted(), color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = "Detected on: ${dateFormat.format(Date(disc.detectedAt))}",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )

                                    if (!disc.status.isResolvedOrWaived) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    selectedForAction = disc
                                                    actionType = "RESOLVE"
                                                    actionNote = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Resolve", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedForAction = disc
                                                    actionType = "WAIVE"
                                                    actionNote = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Admin Waive", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (disc.resolvedBy != null) {
                                        Text("Resolved by: ${disc.resolvedBy} (Note: ${disc.resolutionNote})", color = Color(0xFF10B981), fontSize = 11.sp)
                                    } else if (disc.waivedBy != null) {
                                        Text("Waived by Admin: ${disc.waivedBy} (Reason: ${disc.waiverReason})", color = Color(0xFFC084FC), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Dialog
            if (selectedForAction != null && actionType != null) {
                AlertDialog(
                    onDismissRequest = {
                        selectedForAction = null
                        actionType = null
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val disc = selectedForAction
                                if (disc != null) {
                                    if (actionType == "RESOLVE") {
                                        viewModel.resolveDiscrepancy(disc.discrepancyId, actionNote)
                                    } else {
                                        viewModel.waiveDiscrepancy(disc.discrepancyId, actionNote)
                                    }
                                }
                                selectedForAction = null
                                actionType = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (actionType == "RESOLVE") Color(0xFF2563EB) else Color(0xFF7C3AED)
                            )
                        ) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            selectedForAction = null
                            actionType = null
                        }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    title = { Text(if (actionType == "RESOLVE") "Resolve Discrepancy" else "Waive Discrepancy (Admin)") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Item: ${selectedForAction?.discrepancyNo}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            OutlinedTextField(
                                value = actionNote,
                                onValueChange = { actionNote = it },
                                label = { Text(if (actionType == "RESOLVE") "Resolution Note" else "Administrative Waiver Justification", color = Color(0xFF94A3B8)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = Color(0xFFF8FAFC),
                                    unfocusedTextColor = Color(0xFFF8FAFC),
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF334155)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color(0xFFF8FAFC),
                    textContentColor = Color(0xFFCBD5E1)
                )
            }
        }
    }
}
