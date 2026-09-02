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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLineCondition
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReturnInspectionScreen(
    returnId: String,
    returnLineId: String,
    repository: DeliveryReturnRepository,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var line by remember { mutableStateOf<DeliveryReturnLine?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var acceptedQtyText by remember { mutableStateOf("") }
    var rejectedQtyText by remember { mutableStateOf("") }
    var selectedCondition by remember { mutableStateOf(DeliveryReturnLineCondition.GOOD) }
    var selectedDisposition by remember { mutableStateOf(DeliveryReturnDisposition.RESTOCK) }
    var inspectionNotes by remember { mutableStateOf("") }

    LaunchedEffect(returnId, returnLineId) {
        val linesRes = repository.getReturnLines(returnId)
        if (linesRes is DomainResult.Success) {
            val found = linesRes.data.find { it.returnLineId == returnLineId }
            if (found != null) {
                line = found
                val defaultQty = if (found.receivedQuantity > 0) found.receivedQuantity else found.returnedQuantity
                acceptedQtyText = if (found.acceptedQuantity > 0) found.acceptedQuantity.toString() else defaultQty.toString()
                rejectedQtyText = found.rejectedQuantity.toString()
                selectedCondition = if (found.condition != DeliveryReturnLineCondition.UNKNOWN) found.condition else DeliveryReturnLineCondition.GOOD
                selectedDisposition = if (found.disposition != DeliveryReturnDisposition.PENDING_DECISION) found.disposition else DeliveryReturnDisposition.RESTOCK
                inspectionNotes = found.inspectionNotes ?: ""
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inspect Returned Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(innerPadding))
        } else if (line == null) {
            Text("Return Line not found.", modifier = Modifier.padding(innerPadding))
        } else {
            val itemLine = line!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Product: ${itemLine.productId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Returned Quantity: ${itemLine.returnedQuantity.toInt()}", style = MaterialTheme.typography.bodyMedium)
                            Text("Received Quantity: ${itemLine.receivedQuantity.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = acceptedQtyText,
                        onValueChange = { acceptedQtyText = it },
                        label = { Text("Accepted Quantity *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = rejectedQtyText,
                        onValueChange = { rejectedQtyText = it },
                        label = { Text("Rejected Quantity *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Observed Item Condition *", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DeliveryReturnLineCondition.values()) { cond ->
                            FilterChip(
                                selected = selectedCondition == cond,
                                onClick = { selectedCondition = cond },
                                label = { Text(cond.defaultLabel) }
                            )
                        }
                    }
                }

                item {
                    Text("Recommended Disposition *", style = MaterialTheme.typography.labelLarge)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(DeliveryReturnDisposition.values()) { disp ->
                            FilterChip(
                                selected = selectedDisposition == disp,
                                onClick = { selectedDisposition = disp },
                                label = { Text(disp.defaultLabel) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = inspectionNotes,
                        onValueChange = { inspectionNotes = it },
                        label = { Text("Inspector Notes & Damage Assessment") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val acc = acceptedQtyText.toDoubleOrNull() ?: 0.0
                            val rej = rejectedQtyText.toDoubleOrNull() ?: 0.0
                            isSaving = true
                            errorMessage = null

                            coroutineScope.launch {
                                val result = repository.inspectReturnLine(
                                    returnId = returnId,
                                    returnLineId = returnLineId,
                                    acceptedQuantity = acc,
                                    rejectedQuantity = rej,
                                    condition = selectedCondition,
                                    disposition = selectedDisposition,
                                    inspectionNotes = inspectionNotes.ifBlank { null },
                                    actorId = currentUserId,
                                    callerRole = currentUserRole
                                )
                                isSaving = false
                                if (result is DomainResult.Success) {
                                    onSaved()
                                } else if (result is DomainResult.Error) {
                                    errorMessage = result.message
                                }
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Inspection Result")
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
