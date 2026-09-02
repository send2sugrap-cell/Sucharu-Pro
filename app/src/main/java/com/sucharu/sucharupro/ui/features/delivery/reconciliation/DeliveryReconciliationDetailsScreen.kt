package com.sucharu.sucharupro.ui.features.delivery.reconciliation

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReconciliationDetailsScreen(
    viewModel: DeliveryReconciliationDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reconciliation Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onRefreshCalculation() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh Calculation")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.reconciliation == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Reconciliation record not found.")
            }
        } else {
            val rec = state.reconciliation!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderCard(reconciliation = rec)
                }

                item {
                    ActionButtonsSection(
                        reconciliation = rec,
                        onStart = { viewModel.onStartReconciliation() },
                        onMarkReconciled = { viewModel.onMarkReconciled() },
                        onClose = { viewModel.onCloseReconciliation() }
                    )
                }

                if (state.discrepancies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Detected Discrepancies (${state.discrepancies.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(state.discrepancies, key = { it.discrepancyId }) { disc ->
                        DiscrepancyCard(
                            discrepancy = disc,
                            onResolve = { notes -> viewModel.onResolveDiscrepancy(disc.discrepancyId, notes) }
                        )
                    }
                }

                item {
                    Text(
                        text = "Item-Level Reconciliation (${state.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(state.items, key = { it.reconciliationItemId }) { item ->
                    ReconciliationItemCard(item = item)
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(reconciliation: DeliveryReconciliation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order: ${reconciliation.deliveryOrderId}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                DeliveryReconciliationStatusBadge(status = reconciliation.reconciliationStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Ordered Quantity", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.orderedQuantity}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Delivered Quantity", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.deliveredQuantity}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Accepted POD", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.acceptedPodQuantity}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = "Returned", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.returnedQuantity}", style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text(text = "Outstanding", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.outstandingQuantity}", style = MaterialTheme.typography.bodyMedium, color = if (reconciliation.outstandingQuantity > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
                Column {
                    Text(text = "Discrepancy", style = MaterialTheme.typography.labelSmall)
                    Text(text = "${reconciliation.discrepancyQuantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (reconciliation.discrepancyQuantity > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    reconciliation: DeliveryReconciliation,
    onStart: () -> Unit,
    onMarkReconciled: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (reconciliation.reconciliationStatus == DeliveryReconciliationStatus.OPEN) {
            Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                Text("Start Reconcile")
            }
        }
        if (reconciliation.reconciliationStatus.canEdit && reconciliation.discrepancyQuantity == 0.0 && reconciliation.reconciliationStatus != DeliveryReconciliationStatus.RECONCILED) {
            Button(onClick = onMarkReconciled, modifier = Modifier.weight(1f)) {
                Text("Mark Reconciled")
            }
        }
        if (reconciliation.reconciliationStatus != DeliveryReconciliationStatus.CLOSED) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun DiscrepancyCard(
    discrepancy: DeliveryReconciliationDiscrepancy,
    onResolve: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (discrepancy.isResolved) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = discrepancy.discrepancyType.defaultLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                DeliveryReconciliationDiscrepancySeverityBadge(severity = discrepancy.severity)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = discrepancy.description, style = MaterialTheme.typography.bodySmall)

            if (!discrepancy.isResolved) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onResolve("Resolved during operational review") }) {
                    Text("Resolve Discrepancy")
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Resolved: ${discrepancy.resolutionNotes ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ReconciliationItemCard(item: DeliveryReconciliationItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Product: ${item.productId}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                DeliveryReconciliationItemStatusBadge(status = item.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Ordered: ${item.orderedQuantity}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Delivered: ${item.deliveredQuantity}", style = MaterialTheme.typography.bodySmall)
                Text(text = "POD: ${item.acceptedPodQuantity}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Outstanding: ${item.outstandingQuantity}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
