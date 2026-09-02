package com.sucharu.sucharupro.ui.features.delivery.partial

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.sucharu.sucharupro.domain.model.delivery.partial.DeliverySettlementStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliverySettlementDetailsScreen(
    settlementId: String,
    viewModel: DeliverySettlementDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onRecordPartialDelivery: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(settlementId) {
        viewModel.loadSettlementDetails(settlementId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settlement Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                        IconButton(onClick = { viewModel.recalculateSettlement(settlementId, currentUserId, currentUserRole) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Recalculate")
                        }
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
        } else if (uiState.settlement == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Settlement not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val settlement = uiState.settlement!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Details Card
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
                                    text = "Order: ${settlement.deliveryOrderId}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliverySettlementStatusBadge(status = settlement.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Overall progress
                            val progress = (settlement.totalDeliveredQuantity / settlement.totalOrderedQuantity.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = if (progress >= 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Ordered", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${settlement.totalOrderedQuantity.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("Delivered", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${settlement.totalDeliveredQuantity.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                Column {
                                    Text("Pending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${settlement.totalPendingQuantity.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                }
                                Column {
                                    Text("Completion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text("${settlement.completionPercentage.toInt()}%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Operational Action Bar
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!settlement.status.isTerminal && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                            Button(
                                onClick = { onRecordPartialDelivery(settlement.settlementId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Record Partial Delivery")
                            }
                        }

                        if (!settlement.status.isTerminal && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                            Button(
                                onClick = { viewModel.finalizeSettlement(settlement.settlementId, null, currentUserId, currentUserRole) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finalize")
                            }
                        }
                    }
                }

                // Product Lines Breakdown
                item {
                    Text("Product Breakdown (${uiState.lines.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(uiState.lines) { line ->
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
                                Text(
                                    text = "Product: ${line.productId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliverySettlementStatusBadge(status = line.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ordered: ${line.orderedQuantity.toInt()}", style = MaterialTheme.typography.bodySmall)
                                Text("Delivered: ${line.deliveredQuantity.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                Text("Pending: ${line.pendingQuantity.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Split Dispatches Section
                if (uiState.splitDispatches.isNotEmpty()) {
                    item {
                        Text("Split Dispatches (${uiState.splitDispatches.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(uiState.splitDispatches) { split ->
                        DeliverySplitDispatchCard(split = split)
                    }
                }

                // Audit History Section
                item {
                    Text("Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                items(uiState.events) { event ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(event.eventType.defaultLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(dateFormat.format(Date(event.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
