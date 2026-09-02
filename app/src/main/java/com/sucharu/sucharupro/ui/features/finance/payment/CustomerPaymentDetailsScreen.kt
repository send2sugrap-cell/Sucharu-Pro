package com.sucharu.sucharupro.ui.features.finance.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentDetailsScreen(
    viewModel: CustomerPaymentDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var reasonInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.actionSuccessMessage) {
        uiState.actionSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Customer Payment") },
            text = {
                Column {
                    Text("Provide a reason for cancelling this draft/pending payment:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = { Text("Cancellation reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reasonInput.isNotBlank()) {
                            viewModel.cancelPayment(reasonInput)
                            showCancelDialog = false
                            reasonInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showCancelDialog = false }) { Text("Dismiss") }
            }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Reject Customer Payment") },
            text = {
                Column {
                    Text("Provide a reason for rejecting this payment submission:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reasonInput,
                        onValueChange = { reasonInput = it },
                        placeholder = { Text("Rejection reason...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reasonInput.isNotBlank()) {
                            viewModel.rejectPayment(reasonInput)
                            showRejectDialog = false
                            reasonInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reject")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false }) { Text("Dismiss") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            uiState.payment?.let { pay ->
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(pay.paymentDate))
                val createdStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(pay.createdAt))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header card
                    item {
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
                                    Column {
                                        Text(
                                            text = pay.paymentNo,
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "Customer ID: ${pay.customerId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    CustomerPaymentStatusBadge(status = pay.status)
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Payment Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                        Text(
                                            text = pay.amount.formatted(),
                                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    PaymentMethodBadge(method = pay.paymentMethod)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(text = "Payment Date: $dateStr", style = MaterialTheme.typography.bodySmall)
                                if (!pay.paymentReference.isNullOrBlank()) {
                                    Text(text = "Reference: ${pay.paymentReference}", style = MaterialTheme.typography.bodySmall)
                                }
                                Text(text = "Receivable Obligation: #${pay.receivableId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // Issued Receipt Card
                    uiState.receipt?.let { receipt ->
                        item {
                            CustomerPaymentReceiptCard(receipt = receipt)
                        }
                    }

                    // Lifecycle Actions
                    if (!pay.status.isTerminal) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = "Payment Actions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (pay.status == CustomerPaymentStatus.DRAFT) {
                                        Button(
                                            onClick = viewModel::submitPayment,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Send, contentDescription = "Submit")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Submit for Posting")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    if (pay.status == CustomerPaymentStatus.PENDING || pay.status == CustomerPaymentStatus.DRAFT) {
                                        Button(
                                            onClick = { viewModel.postPayment() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Post")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Post to Ledger & Issue Receipt")
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (pay.status == CustomerPaymentStatus.PENDING) {
                                            OutlinedButton(
                                                onClick = { showRejectDialog = true },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = "Reject")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reject")
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { showCancelDialog = true },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.Cancel, contentDescription = "Cancel")
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Audit events
                    item {
                        Text(
                            text = "Activity Audit Trail",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    if (uiState.activityEvents.isEmpty()) {
                        item {
                            Text(text = "No audit events logged.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        items(uiState.activityEvents, key = { it.eventId }) { event ->
                            CustomerPaymentActivityTimelineItem(event = event)
                        }
                    }
                }
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Payment record not found.", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
