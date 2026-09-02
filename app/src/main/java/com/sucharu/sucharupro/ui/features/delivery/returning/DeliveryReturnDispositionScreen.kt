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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnDisposition
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnLine
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReturnDispositionScreen(
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

    var selectedDisposition by remember { mutableStateOf(DeliveryReturnDisposition.RESTOCK) }
    var warehouseIdText by remember { mutableStateOf("") }
    var locationIdText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    LaunchedEffect(returnId, returnLineId) {
        val linesRes = repository.getReturnLines(returnId, currentUserRole)
        if (linesRes is DomainResult.Success) {
            val target = linesRes.data.find { it.returnLineId == returnLineId }
            if (target != null) {
                line = target
                selectedDisposition = target.disposition
                warehouseIdText = target.warehouseId ?: ""
                locationIdText = target.locationId ?: ""
                notesText = target.inspectionNotes ?: ""
            } else {
                errorMessage = "Return line not found."
            }
        } else if (linesRes is DomainResult.Error) {
            errorMessage = linesRes.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assign Return Disposition") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val targetLine = line
        if (targetLine == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Product: ${targetLine.productId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Accepted Quantity: ${targetLine.acceptedQuantity}")
                        Text(text = "Condition: ${targetLine.condition.defaultLabel}")
                    }
                }
            }

            item {
                Text(
                    text = "Select Disposition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeliveryReturnDisposition.values().forEach { disp ->
                        FilterChip(
                            selected = selectedDisposition == disp,
                            onClick = { selectedDisposition = disp },
                            label = { Text(disp.defaultLabel) },
                            leadingIcon = if (selectedDisposition == disp) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (selectedDisposition.allowsRestock) {
                item {
                    Text(
                        text = "Warehouse Destination (For Restock)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = warehouseIdText,
                        onValueChange = { warehouseIdText = it },
                        label = { Text("Warehouse ID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = locationIdText,
                        onValueChange = { locationIdText = it },
                        label = { Text("Location ID / Bin") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes / Rationale") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            if (errorMessage != null) {
                item {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                errorMessage = null
                                val res = repository.setDisposition(
                                    returnId = returnId,
                                    returnLineId = returnLineId,
                                    disposition = selectedDisposition,
                                    actorId = currentUserId,
                                    callerRole = currentUserRole
                                )
                                if (res is DomainResult.Success) {
                                    if (selectedDisposition.allowsRestock && warehouseIdText.isNotBlank()) {
                                        repository.processRestock(
                                            returnId = returnId,
                                            returnLineId = returnLineId,
                                            warehouseId = warehouseIdText.trim(),
                                            locationId = locationIdText.trim(),
                                            actorId = currentUserId,
                                            callerRole = currentUserRole
                                        )
                                    }
                                    onSaved()
                                } else if (res is DomainResult.Error) {
                                    errorMessage = res.message
                                }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text(if (isSaving) "Saving..." else "Confirm Disposition")
                    }
                }
            }
        }
    }
}
