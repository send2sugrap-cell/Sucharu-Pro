package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentDirection
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdjustmentDetailsScreen(
    viewModel: FinancialAdjustmentDetailsViewModel,
    callerRole: UserRole,
    onNavigateBack: () -> Unit,
    onViewCreditNoteClick: ((String) -> Unit)? = null,
    onViewDebitNoteClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.adjustment?.let { "Adjustment #${it.adjustmentNo}" } ?: "Adjustment Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null && state.adjustment == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = state.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
            state.adjustment != null -> {
                val adjustment = state.adjustment!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Details
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FinancialAdjustmentTypeBadge(type = adjustment.adjustmentType)
                                    FinancialAdjustmentStatusBadge(status = adjustment.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${adjustment.amount.formatted()} ${adjustment.currency}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (adjustment.direction == FinancialAdjustmentDirection.DEBIT) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    }
                                    FinancialAdjustmentDirectionBadge(direction = adjustment.direction)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(text = "Description: ${adjustment.description}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(text = "Reason (${adjustment.reasonCode}): ${adjustment.reason}", fontSize = 13.sp)
                                Text(text = "Business Reference: ${adjustment.referenceType.name} #${adjustment.referenceId}", fontSize = 12.sp)

                                if (!adjustment.customerId.isNullOrBlank()) {
                                    Text(text = "Customer: #${adjustment.customerId}", fontSize = 12.sp)
                                }
                                if (!adjustment.vendorId.isNullOrBlank()) {
                                    Text(text = "Vendor: #${adjustment.vendorId}", fontSize = 12.sp)
                                }
                                if (!adjustment.relatedReceivableId.isNullOrBlank()) {
                                    Text(text = "Settled Receivable: #${adjustment.relatedReceivableId}", fontSize = 12.sp, color = Color(0xFF16A34A))
                                }
                                if (!adjustment.relatedPayableId.isNullOrBlank()) {
                                    Text(text = "Settled Payable: #${adjustment.relatedPayableId}", fontSize = 12.sp, color = Color(0xFF16A34A))
                                }

                                if (!adjustment.financialTransactionId.isNullOrBlank()) {
                                    Text(
                                        text = "Ledger Transaction ID: ${adjustment.financialTransactionId}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Text(text = "Created: ${dateFormat.format(Date(adjustment.createdAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Attached Credit Note / Debit Note cards
                    if (state.creditNote != null) {
                        item {
                            Text(text = "Issued Credit Note", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            CustomerCreditNoteCard(
                                creditNote = state.creditNote!!,
                                onClick = { onViewCreditNoteClick?.invoke(state.creditNote!!.creditNoteId) }
                            )
                        }
                    }

                    if (state.debitNote != null) {
                        item {
                            Text(text = "Issued Debit Note", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            VendorDebitNoteCard(
                                debitNote = state.debitNote!!,
                                onClick = { onViewDebitNoteClick?.invoke(state.debitNote!!.debitNoteId) }
                            )
                        }
                    }

                    // Role Actions
                    item {
                        if (callerRole.isInternal && !adjustment.status.isTerminal) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (adjustment.status == FinancialAdjustmentStatus.DRAFT) {
                                        Button(
                                            onClick = viewModel::submitAdjustment,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Submit")
                                        }
                                    }

                                    if (adjustment.status == FinancialAdjustmentStatus.PENDING) {
                                        Button(
                                            onClick = viewModel::approveAdjustment,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve")
                                        }
                                    }

                                    if (adjustment.status == FinancialAdjustmentStatus.APPROVED || adjustment.status == FinancialAdjustmentStatus.PENDING) {
                                        Button(
                                            onClick = { viewModel.postAdjustment() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Post to Ledger")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (adjustment.status == FinancialAdjustmentStatus.PENDING) {
                                        OutlinedButton(
                                            onClick = { viewModel.rejectAdjustment("Rejected by accounts") },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.cancelAdjustment("Cancelled by user") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }

                    // Activity Audit Trail
                    item {
                        Text(
                            text = "Activity Audit Trail",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.activityEvents, key = { it.eventId }) { event ->
                        FinancialAdjustmentActivityTimelineItem(event = event)
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
