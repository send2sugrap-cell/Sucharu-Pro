package com.sucharu.sucharupro.ui.features.delivery.returning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturn
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipment
import com.sucharu.sucharupro.domain.model.delivery.returning.DeliveryReturnShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryReturnRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryReverseShipmentScreen(
    returnId: String,
    repository: DeliveryReturnRepository,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var deliveryReturn by remember { mutableStateOf<DeliveryReturn?>(null) }
    var reverseShipment by remember { mutableStateOf<DeliveryReturnShipment?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var carrierNameText by remember { mutableStateOf("") }
    var trackingNumberText by remember { mutableStateOf("") }
    var pickupAddressText by remember { mutableStateOf("") }
    var destinationAddressText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf(DeliveryReturnShipmentStatus.READY) }

    LaunchedEffect(returnId) {
        val retRes = repository.getReturn(returnId, currentUserRole)
        if (retRes is DomainResult.Success) {
            deliveryReturn = retRes.data
            val shipRes = repository.getReverseShipment(returnId, currentUserRole)
            if (shipRes is DomainResult.Success) {
                val s = shipRes.data
                reverseShipment = s
                carrierNameText = s.carrierName
                trackingNumberText = s.trackingNumber ?: ""
                pickupAddressText = s.pickupAddress ?: ""
                destinationAddressText = s.destinationAddress ?: ""
                notesText = s.notes ?: ""
                selectedStatus = s.status
            }
        } else if (retRes is DomainResult.Error) {
            errorMessage = retRes.message
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reverse Logistics Shipment") },
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

        val ret = deliveryReturn
        if (ret == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(errorMessage ?: "Return not found", color = MaterialTheme.colorScheme.error)
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
                            text = "Return No: ${ret.returnNo}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Project: ${ret.projectId}")
                        Text(text = "Status: ${ret.status.defaultLabel}")
                    }
                }
            }

            if (reverseShipment == null) {
                item {
                    Text(
                        text = "Create Reverse Shipment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    OutlinedTextField(
                        value = carrierNameText,
                        onValueChange = { carrierNameText = it },
                        label = { Text("Carrier / Logistics Partner *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = trackingNumberText,
                        onValueChange = { trackingNumberText = it },
                        label = { Text("Tracking Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = pickupAddressText,
                        onValueChange = { pickupAddressText = it },
                        label = { Text("Customer Pickup Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = destinationAddressText,
                        onValueChange = { destinationAddressText = it },
                        label = { Text("Warehouse Destination Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Transit Notes / Instructions") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                errorMessage = null
                                val shipment = DeliveryReturnShipment(
                                    reverseShipmentId = UUID.randomUUID().toString(),
                                    returnId = returnId,
                                    projectId = ret.projectId,
                                    carrierName = carrierNameText.trim(),
                                    trackingNumber = trackingNumberText.trim().ifBlank { null },
                                    pickupAddress = pickupAddressText.trim().ifBlank { null },
                                    destinationAddress = destinationAddressText.trim().ifBlank { null },
                                    status = selectedStatus,
                                    notes = notesText.trim().ifBlank { null },
                                    createdBy = currentUserId
                                )
                                val res = repository.createReverseShipment(
                                    shipment = shipment,
                                    actorId = currentUserId,
                                    callerRole = currentUserRole
                                )
                                if (res is DomainResult.Success) {
                                    onSaved()
                                } else if (res is DomainResult.Error) {
                                    errorMessage = res.message
                                }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && carrierNameText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSaving) "Creating..." else "Create Reverse Shipment")
                    }
                }
            } else {
                val currentShipment = reverseShipment!!
                item {
                    Text(
                        text = "Reverse Shipment Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Carrier: ${currentShipment.carrierName}")
                    if (!currentShipment.trackingNumber.isNullOrBlank()) {
                        Text(text = "Tracking: ${currentShipment.trackingNumber}")
                    }
                    Text(text = "Current Status: ${currentShipment.status.defaultLabel}")
                }

                item {
                    Text(
                        text = "Update Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DeliveryReturnShipmentStatus.values().forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st,
                                onClick = { selectedStatus = st },
                                label = { Text(st.defaultLabel) },
                                leadingIcon = if (selectedStatus == st) {
                                    { Icon(Icons.Default.Check, contentDescription = null) }
                                } else null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSaving = true
                                errorMessage = null
                                val res = repository.updateReverseShipmentStatus(
                                    returnId = returnId,
                                    newStatus = selectedStatus,
                                    notes = notesText.trim().ifBlank { null },
                                    actorId = currentUserId,
                                    callerRole = currentUserRole
                                )
                                if (res is DomainResult.Success) {
                                    onSaved()
                                } else if (res is DomainResult.Error) {
                                    errorMessage = res.message
                                }
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && selectedStatus != currentShipment.status,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSaving) "Updating..." else "Update Transit Status")
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
