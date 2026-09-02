package com.sucharu.sucharupro.ui.features.finance.periodclose

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
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
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenRequest
import com.sucharu.sucharupro.domain.model.finance.FinancialPeriodReopenStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PeriodReopenRequestScreen(
    periodId: String,
    viewModel: AccountingPeriodViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.detailsState.collectAsState()
    var selectedForReject by remember { mutableStateOf<FinancialPeriodReopenRequest?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    LaunchedEffect(periodId) {
        viewModel.loadPeriodDetails(periodId)
    }

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
                    text = "Period Reopen Audit Requests",
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
                    if (state.reopenRequests.isEmpty()) {
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
                                    Text("No reopen requests submitted for this period.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                }
                            }
                        }
                    } else {
                        items(state.reopenRequests) { req ->
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
                                        Text(req.requestNo, color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        PeriodReopenStatusBadge(status = req.status)
                                    }

                                    Text("Reason: ${req.reason}", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                                    Text("Requested by: ${req.requestedBy} on ${dateFormat.format(Date(req.requestedAt))}", color = Color(0xFF94A3B8), fontSize = 11.sp)

                                    if (req.status == FinancialPeriodReopenStatus.PENDING) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.approveReopenRequest(req.requestId, periodId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Approve & Reopen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedForReject = req
                                                    rejectionReason = ""
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Reject", fontSize = 11.sp)
                                            }
                                        }
                                    } else if (req.status == FinancialPeriodReopenStatus.APPROVED) {
                                        Text("Approved by Admin: ${req.approvedBy}", color = Color(0xFF6EE7B7), fontSize = 11.sp)
                                    } else if (req.status == FinancialPeriodReopenStatus.REJECTED) {
                                        Text("Rejection Reason: ${req.rejectionReason}", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Reject Dialog
            if (selectedForReject != null) {
                AlertDialog(
                    onDismissRequest = { selectedForReject = null },
                    confirmButton = {
                        Button(
                            onClick = {
                                val req = selectedForReject
                                if (req != null) {
                                    viewModel.rejectReopenRequest(req.requestId, periodId, rejectionReason)
                                }
                                selectedForReject = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Reject")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedForReject = null }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    title = { Text("Reject Reopen Request") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Provide rejection justification:", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            OutlinedTextField(
                                value = rejectionReason,
                                onValueChange = { rejectionReason = it },
                                label = { Text("Rejection Reason", color = Color(0xFF94A3B8)) },
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
