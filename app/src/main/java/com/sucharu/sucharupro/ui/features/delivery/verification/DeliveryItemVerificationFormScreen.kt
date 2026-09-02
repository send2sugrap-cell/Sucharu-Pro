package com.sucharu.sucharupro.ui.features.delivery.verification

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.user.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryItemVerificationFormScreen(
    projectId: String,
    viewModel: DeliveryItemVerificationFormViewModel,
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
                title = { Text("New Delivery Item Verification") },
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
                    value = uiState.verificationNo,
                    onValueChange = { viewModel.onVerificationNoChanged(it) },
                    label = { Text("Verification Number *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Select Dispatched Execution *", style = MaterialTheme.typography.labelLarge)
                if (uiState.availableDispatches.isEmpty()) {
                    Text(
                        "No eligible (DISPATCHED) records found.",
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
                                onClick = { viewModel.selectDispatch(dispatch.dispatchExecutionId) },
                                label = { Text(dispatch.dispatchNo) }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.remarks,
                    onValueChange = { viewModel.onRemarksChanged(it) },
                    label = { Text("Verification Remarks / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                Text(
                    "Physical Delivery Item Verification (${uiState.lines.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(uiState.lines) { index, line ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Product: ${line.productId}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Expected (Dispatched): ${line.expectedQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = line.verifiedQuantity.toString(),
                            onValueChange = {
                                val qty = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateVerifiedQuantity(index, qty)
                            },
                            label = { Text("Verified Quantity *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Missing / Not Present", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = line.isMissing,
                                onCheckedChange = { viewModel.updateMissing(index, it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Damage Reported", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = line.isDamaged,
                                onCheckedChange = { viewModel.updateDamage(index, it, if (it) 1.0 else 0.0) }
                            )
                        }

                        if (line.isDamaged) {
                            OutlinedTextField(
                                value = line.damagedQuantity.toString(),
                                onValueChange = {
                                    val qty = it.toDoubleOrNull() ?: 0.0
                                    viewModel.updateDamage(index, true, qty)
                                },
                                label = { Text("Damaged Quantity") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveVerification(currentUserId, currentUserRole) },
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
                        Text("Create Delivery Verification")
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
