package com.sucharu.sucharupro.ui.features.delivery.dispatch

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.dispatch.DispatchExecutionStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatchExecutionDetailsScreen(
    dispatchExecutionId: String,
    viewModel: DispatchExecutionDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(dispatchExecutionId) {
        viewModel.loadDispatchDetails(dispatchExecutionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.dispatch?.dispatchNo ?: "Dispatch Execution") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.dispatch == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Dispatch execution not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val dispatch = uiState.dispatch!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
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
                                Text(
                                    text = dispatch.dispatchNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                DispatchExecutionStatusBadge(status = dispatch.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Delivery Challan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        uiState.challan?.challanNo ?: dispatch.deliveryChallanId.take(8),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column {
                                    Text("Dispatch Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    DispatchExecutionTypeBadge(type = dispatch.dispatchType)
                                }
                                Column {
                                    Text("Warehouse / Loc", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        "${dispatch.sourceWarehouseId} / ${dispatch.sourceLocationId}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val stockId = dispatch.stockOutId
                            if (stockId != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Stock-Out Reference", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Text(stockId, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }

                            val notes = dispatch.notes
                            if (notes != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Text(notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (dispatch.status) {
                            DispatchExecutionStatus.DRAFT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.submitDispatch(dispatch.dispatchExecutionId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Submit")
                                    }
                                }
                            }
                            DispatchExecutionStatus.PENDING -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.approveDispatch(dispatch.dispatchExecutionId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                            DispatchExecutionStatus.APPROVED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markReadyForExecution(dispatch.dispatchExecutionId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Prepare Dispatch")
                                    }
                                }
                            }
                            DispatchExecutionStatus.READY_FOR_EXECUTION -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.executeDispatch(dispatch.dispatchExecutionId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Execute & Stock Out")
                                    }
                                }
                            }
                            else -> {}
                        }

                        if (!dispatch.status.isTerminal && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                            OutlinedButton(
                                onClick = { viewModel.cancelDispatch(dispatch.dispatchExecutionId, currentUserId, "User requested cancellation", currentUserRole) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Lines & Live Inventory Availability Section
                item {
                    Text(
                        "Dispatch Lines & Stock Availability (${uiState.lines.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.lines) { line ->
                    val availableStock = uiState.stockAvailabilityMap[line.dispatchExecutionLineId] ?: 0
                    val isStockSufficient = availableStock >= line.dispatchQuantity.toInt()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Product: ${line.productId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Dispatch Qty: ${line.dispatchQuantity}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Location: ${line.sourceLocationId} • Batch: ${line.batchId ?: "N/A"} • Lot: ${line.lotId ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    "Available: $availableStock",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStockSufficient) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }

                // Audit History Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Dispatch Activity History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.activityEvents) { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                event.activityType.defaultLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                dateFormat.format(Date(event.performedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        val details = event.details
                        if (details != null) {
                            Text(details, style = MaterialTheme.typography.bodySmall)
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
