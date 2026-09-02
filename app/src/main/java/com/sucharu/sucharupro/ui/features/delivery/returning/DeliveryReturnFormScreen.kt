package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnPriority
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnReason
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnType
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReturnFormScreen(
    projectId: String,
    deliveryOrderId: String,
    viewModel: DeliveryReturnFormViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(projectId, deliveryOrderId) {
        viewModel.initialize(projectId, deliveryOrderId)
    }

    LaunchedEffect(uiState.isSavedSuccessfully) {
        if (uiState.isSavedSuccessfully) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Delivery Return") },
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
                    value = uiState.returnNo,
                    onValueChange = { viewModel.onReturnNoChanged(it) },
                    label = { Text("Return Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Select Product Item *", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.availableOrderLines) { line ->
                        FilterChip(
                            selected = uiState.selectedDoLineId == line.lineId,
                            onClick = { viewModel.onLineSelected(line.lineId) },
                            label = { Text("${line.productId} (Ordered: ${line.requestedQuantity.toInt()})") }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.returnQuantityText,
                    onValueChange = { viewModel.onQuantityChanged(it) },
                    label = { Text("Returned Quantity *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Return Classification / Type *", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DeliveryReturnType.values()) { type ->
                        FilterChip(
                            selected = uiState.returnType == type,
                            onClick = { viewModel.onTypeSelected(type) },
                            label = { Text(type.defaultLabel) }
                        )
                    }
                }
            }

            item {
                Text("Return Reason *", style = MaterialTheme.typography.labelLarge)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(DeliveryReturnReason.values()) { reason ->
                        FilterChip(
                            selected = uiState.returnReason == reason,
                            onClick = { viewModel.onReasonSelected(reason) },
                            label = { Text(reason.defaultLabel) }
                        )
                    }
                }
            }

            item {
                Text("Priority *", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DeliveryReturnPriority.values().forEach { priority ->
                        FilterChip(
                            selected = uiState.priority == priority,
                            onClick = { viewModel.onPrioritySelected(priority) },
                            label = { Text(priority.defaultLabel) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    label = { Text("Operational Notes / Customer Remarks") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.submitCreateReturn(currentUserId, currentUserRole) },
                    enabled = !uiState.isSaving && uiState.returnNo.isNotBlank() && uiState.selectedDoLineId.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Delivery Return", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
