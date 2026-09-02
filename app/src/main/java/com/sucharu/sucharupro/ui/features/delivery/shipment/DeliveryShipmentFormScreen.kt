package com.sucharu.sucharupro.ui.features.delivery.shipment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentPriority
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentType
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryShipmentFormScreen(
    projectId: String,
    viewModel: DeliveryShipmentFormViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    preselectedDispatchId: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.initialize(projectId, preselectedDispatchId)
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Delivery Shipment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (uiState.errorMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.shipmentNo,
                    onValueChange = { viewModel.onShipmentNoChanged(it) },
                    label = { Text("Shipment Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Select Dispatched Execution *", style = MaterialTheme.typography.labelLarge)
                if (uiState.availableDispatches.isEmpty()) {
                    Text(
                        "No eligible (DISPATCHED) records available.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(uiState.availableDispatches) { dispatch ->
                            FilterChip(
                                selected = uiState.selectedDispatchId == dispatch.dispatchExecutionId,
                                onClick = { viewModel.onDispatchSelected(dispatch.dispatchExecutionId) },
                                label = { Text(dispatch.dispatchNo) }
                            )
                        }
                    }
                }
            }

            item {
                Text("Shipment Method / Type", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DeliveryShipmentType.values()) { type ->
                        FilterChip(
                            selected = uiState.shipmentType == type,
                            onClick = { viewModel.onTypeChanged(type) },
                            label = { Text(type.defaultLabel) }
                        )
                    }
                }
            }

            item {
                Text("Priority Level", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DeliveryShipmentPriority.values()) { prio ->
                        FilterChip(
                            selected = uiState.priority == prio,
                            onClick = { viewModel.onPriorityChanged(prio) },
                            label = { Text(prio.defaultLabel) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.carrierName,
                    onValueChange = { viewModel.onCarrierNameChanged(it) },
                    label = { Text("Carrier Name (e.g. Steadfast, Pathao, Internal Van)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.trackingNumber,
                    onValueChange = { viewModel.onTrackingNumberChanged(it) },
                    label = { Text("Tracking Number / Waybill #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.destinationAddress,
                    onValueChange = { viewModel.onDestinationAddressChanged(it) },
                    label = { Text("Destination Address Snapshot") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.destinationContactName,
                    onValueChange = { viewModel.onDestinationContactNameChanged(it) },
                    label = { Text("Recipient Contact Person") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.destinationContactPhone,
                    onValueChange = { viewModel.onDestinationContactPhoneChanged(it) },
                    label = { Text("Recipient Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Operational Remarks / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveShipment(currentUserId, currentUserRole) },
                    enabled = !uiState.isSaving && uiState.selectedDispatchId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Delivery Shipment")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
