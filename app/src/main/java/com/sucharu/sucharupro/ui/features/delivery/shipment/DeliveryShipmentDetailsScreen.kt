package com.sucharu.sucharupro.ui.features.delivery.shipment

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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.sucharu.sucharupro.domain.model.delivery.shipment.DeliveryShipmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryShipmentDetailsScreen(
    shipmentId: String,
    viewModel: DeliveryShipmentDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(shipmentId) {
        viewModel.loadShipmentDetails(shipmentId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.shipment?.shipmentNo ?: "Shipment Details") },
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
        } else if (uiState.shipment == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Shipment not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val shipment = uiState.shipment!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Details
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
                                    text = shipment.shipmentNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliveryShipmentStatusBadge(status = shipment.currentStatus)
                            }

                            if (!shipment.trackingNumber.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tracking #: ${shipment.trackingNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DeliveryShipmentTypeBadge(type = shipment.shipmentType)
                                DeliveryShipmentPriorityBadge(priority = shipment.priority)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Dispatch", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(shipment.dispatchExecutionId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Challan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(shipment.deliveryChallanId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Order", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(shipment.deliveryOrderId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Destination & Carrier Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Delivery Destination", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Address: ${shipment.destinationAddress ?: "Standard Delivery Address"}", style = MaterialTheme.typography.bodyMedium)
                            if (!shipment.destinationContactName.isNullOrBlank()) {
                                Text("Contact: ${shipment.destinationContactName} (${shipment.destinationContactPhone ?: "N/A"})", style = MaterialTheme.typography.bodySmall)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                            Text("Carrier Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Carrier: ${shipment.carrierName ?: "Internal Delivery Team"}", style = MaterialTheme.typography.bodyMedium)
                            if (!shipment.carrierReference.isNullOrBlank()) {
                                Text("Carrier Ref: ${shipment.carrierReference}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Operational Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (shipment.currentStatus) {
                            DeliveryShipmentStatus.DRAFT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markReady(shipment.shipmentId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Ready")
                                    }
                                }
                            }
                            DeliveryShipmentStatus.READY -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markDispatched(shipment.shipmentId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Dispatched")
                                    }
                                }
                            }
                            DeliveryShipmentStatus.DISPATCHED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markInTransit(shipment.shipmentId, "Departed Origin Hub", null, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8))
                                    ) {
                                        Text("In Transit")
                                    }
                                }
                            }
                            DeliveryShipmentStatus.IN_TRANSIT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markOutForDelivery(shipment.shipmentId, "With Local Courier", null, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1))
                                    ) {
                                        Text("Out for Delivery")
                                    }
                                }
                            }
                            DeliveryShipmentStatus.OUT_FOR_DELIVERY, DeliveryShipmentStatus.DELIVERY_ATTEMPTED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markDelivered(shipment.shipmentId, null, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Mark Delivered")
                                    }
                                }
                            }
                            else -> {}
                        }

                        if (!shipment.currentStatus.isTerminal && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                            OutlinedButton(
                                onClick = { viewModel.cancelShipment(shipment.shipmentId, "Cancelled by user", currentUserId, currentUserRole) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Delivery Attempts Section
                if (uiState.deliveryAttempts.isNotEmpty()) {
                    item {
                        Text(
                            "Delivery Attempts (${uiState.deliveryAttempts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(uiState.deliveryAttempts) { attempt ->
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
                                    Text("Attempt #${attempt.attemptNo}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    DeliveryShipmentAttemptStatusBadge(status = attempt.status)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Time: ${dateFormat.format(Date(attempt.attemptedAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                if (!attempt.reason.isNullOrBlank()) {
                                    Text("Reason: ${attempt.reason}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD32F2F))
                                }
                            }
                        }
                    }
                }

                // Tracking Timeline Section
                item {
                    Text(
                        "Tracking Timeline (${uiState.trackingEvents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    DeliveryShipmentTrackingTimeline(events = uiState.trackingEvents)
                }

                // Audit History Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Audit Trail",
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
                            Text(event.activityType.defaultLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(dateFormat.format(Date(event.performedAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
