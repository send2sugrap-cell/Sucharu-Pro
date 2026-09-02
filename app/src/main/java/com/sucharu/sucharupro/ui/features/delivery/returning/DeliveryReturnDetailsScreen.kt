package com.sucharu.sucharupro.ui.features.delivery.returning

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReturnDetailsScreen(
    returnId: String,
    viewModel: DeliveryReturnDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    onInspectLineClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(returnId) {
        viewModel.loadDetails(returnId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.returnItem?.returnNo ?: "Delivery Return Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.returnItem == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Delivery Return not found.", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                val ret = uiState.returnItem!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ret.returnNo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DeliveryReturnPriorityBadge(priority = ret.priority)
                                        DeliveryReturnStatusBadge(status = ret.status)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Delivery Order: ${ret.deliveryOrderId}", style = MaterialTheme.typography.bodyMedium)
                                Text("Customer: ${ret.customerId ?: "N/A"}", style = MaterialTheme.typography.bodyMedium)
                                Text("Type: ${ret.returnType.defaultLabel}", style = MaterialTheme.typography.bodyMedium)
                                Text("Reason: ${ret.returnReason.defaultLabel}", style = MaterialTheme.typography.bodyMedium)
                                if (ret.rejectionReason != null) {
                                    Text("Rejection Reason: ${ret.rejectionReason}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // Lifecycle Actions
                    item {
                        ReturnLifecycleActions(
                            status = ret.status,
                            currentUserRole = currentUserRole,
                            isActionInProgress = uiState.isActionInProgress,
                            onSubmit = { viewModel.submitReturn(ret.returnId, currentUserId, currentUserRole) },
                            onApprove = { viewModel.approveReturn(ret.returnId, currentUserId, currentUserRole) },
                            onReject = { viewModel.rejectReturn(ret.returnId, "Rejected by authority", currentUserId, currentUserRole) },
                            onStartReceiving = { viewModel.startReceiving(ret.returnId, currentUserId, currentUserRole) },
                            onReceive = { viewModel.receiveReturn(ret.returnId, currentUserId, currentUserRole) },
                            onStartInspection = { viewModel.startInspection(ret.returnId, currentUserId, currentUserRole) },
                            onCompleteInspection = { viewModel.completeInspection(ret.returnId, currentUserId, currentUserRole) },
                            onProcessRestock = { viewModel.processAllRestock(ret.returnId, "WH-DEFAULT", "LOC-DEFAULT", currentUserId, currentUserRole) },
                            onComplete = { viewModel.completeReturn(ret.returnId, currentUserId, currentUserRole) },
                            onCancel = { viewModel.cancelReturn(ret.returnId, "Cancelled by user", currentUserId, currentUserRole) }
                        )
                    }

                    // Return Item Lines
                    item {
                        Text("Returned Products (${uiState.lines.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(uiState.lines) { line ->
                        ReturnLineCard(
                            line = line,
                            canInspect = ret.status.canInspect && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE, UserRole.QC_INSPECTOR),
                            onInspectClick = { onInspectLineClick(ret.returnId, line.returnLineId) }
                        )
                    }

                    // Activity Audit Timeline
                    item {
                        Text("Audit & Lifecycle Timeline (${uiState.events.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(uiState.events) { event ->
                        ReturnEventCard(event = event)
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnLifecycleActions(
    status: DeliveryReturnStatus,
    currentUserRole: UserRole,
    isActionInProgress: Boolean,
    onSubmit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onStartReceiving: () -> Unit,
    onReceive: () -> Unit,
    onStartInspection: () -> Unit,
    onCompleteInspection: () -> Unit,
    onProcessRestock: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (status) {
            DeliveryReturnStatus.DRAFT -> {
                Button(onClick = onSubmit, enabled = !isActionInProgress) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Text(" Submit")
                }
                OutlinedButton(onClick = onCancel, enabled = !isActionInProgress) {
                    Text("Cancel")
                }
            }
            DeliveryReturnStatus.PENDING -> {
                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                    Button(onClick = onApprove, enabled = !isActionInProgress) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Text(" Approve")
                    }
                    Button(onClick = onReject, enabled = !isActionInProgress, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Text(" Reject")
                    }
                }
            }
            DeliveryReturnStatus.APPROVED -> {
                Button(onClick = onStartReceiving, enabled = !isActionInProgress) {
                    Text("Start Receiving")
                }
            }
            DeliveryReturnStatus.RECEIVING -> {
                Button(onClick = onReceive, enabled = !isActionInProgress) {
                    Text("Confirm Receipt")
                }
            }
            DeliveryReturnStatus.RECEIVED -> {
                Button(onClick = onStartInspection, enabled = !isActionInProgress) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text(" Start Inspection")
                }
            }
            DeliveryReturnStatus.INSPECTING -> {
                Button(onClick = onCompleteInspection, enabled = !isActionInProgress) {
                    Text("Complete Inspection")
                }
            }
            DeliveryReturnStatus.INSPECTED, DeliveryReturnStatus.DISPOSITION_PENDING -> {
                Button(onClick = onProcessRestock, enabled = !isActionInProgress) {
                    Icon(Icons.Default.Inventory, contentDescription = null)
                    Text(" Process Restock (Stock-In)")
                }
                Button(onClick = onComplete, enabled = !isActionInProgress) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(" Complete Return")
                }
            }
            DeliveryReturnStatus.PROCESSING -> {
                Button(onClick = onComplete, enabled = !isActionInProgress) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(" Complete Return")
                }
            }
            DeliveryReturnStatus.COMPLETED, DeliveryReturnStatus.CANCELLED, DeliveryReturnStatus.REJECTED -> {
                // Terminal state
            }
        }
    }
}

@Composable
private fun ReturnLineCard(
    line: DeliveryReturnLine,
    canInspect: Boolean,
    onInspectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Product: ${line.productId}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DeliveryReturnConditionBadge(condition = line.condition)
                    DeliveryReturnDispositionBadge(disposition = line.disposition)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Returned: ${line.returnedQuantity.toInt()}", style = MaterialTheme.typography.bodySmall)
                Text("Received: ${line.receivedQuantity.toInt()}", style = MaterialTheme.typography.bodySmall)
                Text("Accepted: ${line.acceptedQuantity.toInt()}", style = MaterialTheme.typography.bodySmall)
                Text("Rejected: ${line.rejectedQuantity.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
            if (line.isRestocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Restocked: ${line.restockedQuantity.toInt()} pcs (Stock-In ID: ${line.restockMovementId})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (canInspect) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onInspectClick) {
                    Text("Record Inspection / Disposition")
                }
            }
        }
    }
}

@Composable
private fun ReturnEventCard(
    event: DeliveryReturnActivityEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(event.activityType.defaultLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("By: ${event.actorId}", style = MaterialTheme.typography.labelSmall)
            }
            val notes = event.notes
            if (notes != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
