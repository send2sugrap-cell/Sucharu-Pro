package com.sucharu.sucharupro.ui.features.finance.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialTransactionDetailsScreen(
    viewModel: FinancialTransactionDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.transaction?.transactionNo ?: "Transaction Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val transaction = uiState.transaction
            if (transaction == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Transaction not found.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = transaction.transactionNo,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                FinancialStatusBadge(status = transaction.transactionStatus)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = transaction.amount.formatted(),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Type: ${transaction.transactionType.defaultLabel}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                FinancialEntryTypeBadge(entryType = transaction.entryType)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Reference: ${transaction.referenceType.name} #${transaction.referenceId}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (!transaction.customerId.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Customer ID: ${transaction.customerId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Description: ${transaction.description}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Action Buttons based on status
                    if (!transaction.transactionStatus.isTerminal) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Lifecycle Actions",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (transaction.transactionStatus == FinancialTransactionStatus.DRAFT) {
                                        Button(
                                            onClick = { viewModel.submitTransaction() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Submit for Approval")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.cancelTransaction("Cancelled by user") },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel")
                                        }
                                    } else if (transaction.transactionStatus == FinancialTransactionStatus.PENDING) {
                                        Button(
                                            onClick = { viewModel.postTransaction() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Post to Ledger")
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.rejectTransaction("Rejected by reviewer") },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Ledger Entries Section
                    if (uiState.ledgerEntries.isNotEmpty()) {
                        Text(
                            text = "Authoritative Ledger Entries",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        uiState.ledgerEntries.forEach { entry ->
                            FinancialLedgerEntryCard(entry = entry)
                        }
                    }

                    // Activity Timeline Section
                    if (uiState.activityEvents.isNotEmpty()) {
                        Text(
                            text = "Audit & Activity Timeline",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.activityEvents.forEach { event ->
                                    FinancialActivityTimelineItem(event = event)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
