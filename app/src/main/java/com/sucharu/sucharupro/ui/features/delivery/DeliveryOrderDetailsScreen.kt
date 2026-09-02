package com.sucharu.sucharupro.ui.features.delivery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOrderDetailsScreen(
    deliveryOrderId: String,
    viewModel: DeliveryOrderDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(deliveryOrderId) {
        viewModel.loadOrderDetails(deliveryOrderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.order?.deliveryOrderNo ?: "Delivery Order Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
        } else if (uiState.order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Order not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val order = uiState.order!!

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
                                    text = order.deliveryOrderNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliveryOrderStatusBadge(status = order.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Delivery Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(order.deliveryType.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Priority", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    DeliveryPriorityBadge(priority = order.priority)
                                }
                                Column {
                                    Text("Requested Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(order.requestedDeliveryDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val notes = order.notes
                            if (notes != null) {
                                Spacer(modifier = Modifier.height(10.dp))
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
                        when (order.status) {
                            DeliveryOrderStatus.DRAFT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.submitOrder(order.deliveryOrderId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Submit")
                                    }
                                }
                            }
                            DeliveryOrderStatus.PENDING -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.approveOrder(order.deliveryOrderId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                            DeliveryOrderStatus.APPROVED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markReadyForDispatch(order.deliveryOrderId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ready for Dispatch")
                                    }
                                }
                            }
                            DeliveryOrderStatus.READY_FOR_DISPATCH -> {
                                if (uiState.dispatchRequest == null && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = {
                                            viewModel.createDispatchRequest(
                                                deliveryOrderId = order.deliveryOrderId,
                                                projectId = order.projectId,
                                                priority = order.priority,
                                                notes = "Requested via UI",
                                                requestedBy = currentUserId,
                                                callerRole = currentUserRole
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Create Dispatch Request")
                                    }
                                }
                            }
                            else -> {}
                        }

                        if (order.status in listOf(DeliveryOrderStatus.DRAFT, DeliveryOrderStatus.PENDING, DeliveryOrderStatus.APPROVED, DeliveryOrderStatus.READY_FOR_DISPATCH) &&
                            currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelOrder(order.deliveryOrderId, currentUserId, "User requested cancellation", currentUserRole) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Dispatch Request Card
                if (uiState.dispatchRequest != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Dispatch Request",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    DispatchRequestStatusBadge(status = uiState.dispatchRequest!!.status)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Requested By: ${uiState.dispatchRequest!!.requestedBy}", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Requested At: ${dateFormat.format(Date(uiState.dispatchRequest!!.requestedAt))}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Items Section
                item {
                    Text(
                        "Order Lines (${uiState.lines.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.lines) { line ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Product ID: ${line.productId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                val lineNotes = line.notes
                                if (lineNotes != null) {
                                    Text(lineNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Text(
                                "Qty: ${line.requestedQuantity}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Audit History Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Activity History",
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
                        Divider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
