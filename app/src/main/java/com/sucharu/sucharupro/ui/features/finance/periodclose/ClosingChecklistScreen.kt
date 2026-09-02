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
import androidx.compose.material3.Surface
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
import com.sucharu.sucharupro.domain.model.finance.AccountingPeriodStatus

@Composable
fun ClosingChecklistScreen(
    periodId: String,
    viewModel: AccountingPeriodViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSnapshot: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.detailsState.collectAsState()
    var showReopenDialog by remember { mutableStateOf(false) }
    var reopenReason by remember { mutableStateOf("") }

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
                Column {
                    Text(
                        text = "Period Closing Review & Controls",
                        color = Color(0xFFF8FAFC),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.period?.periodName ?: periodId,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
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
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status Overview Card
                    item {
                        val readiness = state.readiness
                        val period = state.period
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("READINESS STATUS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = if (readiness?.status?.canProceedWithClosing == true) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = readiness?.status?.defaultLabel ?: "NOT EVALUATED",
                                            color = if (readiness?.status?.canProceedWithClosing == true) Color(0xFF6EE7B7) else Color(0xFFFCA5A5),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (readiness?.blockerReasons?.isNotEmpty() == true) {
                                    Text(
                                        text = "Closing Blocked by ${readiness.blockerReasons.size} issue(s):",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    readiness.blockerReasons.forEach { b ->
                                        Text("• ${b.defaultLabel}", color = Color(0xFFF87171), fontSize = 11.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Action Buttons
                                when (period?.status) {
                                    AccountingPeriodStatus.OPEN, AccountingPeriodStatus.REOPENED -> {
                                        Button(
                                            onClick = { viewModel.submitForClosing(period.periodId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Submit for Closing Review", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    AccountingPeriodStatus.CLOSING -> {
                                        Button(
                                            onClick = { viewModel.closePeriod(period.periodId) },
                                            enabled = readiness?.status?.canProceedWithClosing == true,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981),
                                                disabledContainerColor = Color(0xFF334155)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Approve & Close Accounting Period (Lock)", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    AccountingPeriodStatus.CLOSED -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { onNavigateToSnapshot(period.periodId) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("View Snapshot")
                                            }

                                            Button(
                                                onClick = { showReopenDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Request Reopen")
                                            }
                                        }
                                    }
                                    null -> {}
                                }
                            }
                        }
                    }

                    // Checklist Items
                    item {
                        Text(
                            text = "Mandatory Pre-Closing Checklist (${state.readiness?.checklistItems?.size ?: 0})",
                            color = Color(0xFFF8FAFC),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val items = state.readiness?.checklistItems ?: emptyList()
                    items(items) { item ->
                        ChecklistItemCard(item = item)
                    }
                }
            }

            // Reopen Request Dialog
            if (showReopenDialog) {
                AlertDialog(
                    onDismissRequest = { showReopenDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.createReopenRequest(periodId, reopenReason)
                                showReopenDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                        ) {
                            Text("Submit Request")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReopenDialog = false }) {
                            Text("Cancel", color = Color(0xFF94A3B8))
                        }
                    },
                    title = { Text("Request Period Reopen") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Reopening a locked fiscal period requires justification and Administrator approval.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            OutlinedTextField(
                                value = reopenReason,
                                onValueChange = { reopenReason = it },
                                label = { Text("Audit / Adjustment Justification", color = Color(0xFF94A3B8)) },
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
