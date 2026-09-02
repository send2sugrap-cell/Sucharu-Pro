package com.sucharu.sucharupro.ui.features.finance.payable

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
import com.sucharu.sucharupro.domain.model.finance.VendorPayableStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPayableDetailsScreen(
    viewModel: VendorPayableDetailsViewModel,
    callerRole: UserRole,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.payable?.let { "Payable #${it.payableNo}" } ?: "Payable Details") },
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null && state.payable == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
            state.payable != null -> {
                val payable = state.payable!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Overview Card
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
                                    Column {
                                        Text(text = "Vendor ID", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = payable.vendorId, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                    VendorPayableStatusBadge(status = payable.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(text = "Total Liability", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "${payable.originalAmount.formatted()} ${payable.currency}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text(text = "Settled Paid", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = "${payable.settledAmount.formatted()} ${payable.currency}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "Outstanding Due", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${payable.outstandingAmount.formatted()} ${payable.currency}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (payable.outstandingAmount.isPositive()) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(text = "Reference Type: ${payable.referenceType.name}", fontSize = 13.sp)
                                Text(text = "Reference ID: ${payable.referenceId}", fontSize = 13.sp)
                                if (!payable.supplierInvoiceNo.isNullOrBlank()) {
                                    Text(text = "Supplier Bill/Invoice: ${payable.supplierInvoiceNo}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(text = "Due Date: ${dateFormat.format(Date(payable.dueDate))}", fontSize = 13.sp)
                                Text(text = "Description: ${payable.description}", fontSize = 13.sp)

                                if (!payable.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Notes: ${payable.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Action Controls
                    item {
                        if (callerRole.isInternal && !payable.status.isTerminal) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (payable.status == VendorPayableStatus.DRAFT) {
                                    Button(
                                        onClick = viewModel::submitPayable,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Submit")
                                    }
                                }

                                if (payable.status == VendorPayableStatus.PENDING || payable.status == VendorPayableStatus.DRAFT) {
                                    Button(
                                        onClick = viewModel::approvePayable,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve")
                                    }
                                }

                                OutlinedButton(
                                    onClick = { viewModel.cancelPayable("Cancelled by user") },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }

                    // Audit Trail Timeline
                    item {
                        Text(
                            text = "Activity Audit Trail",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.activityEvents, key = { it.eventId }) { event ->
                        VendorPayableActivityTimelineItem(event = event)
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
