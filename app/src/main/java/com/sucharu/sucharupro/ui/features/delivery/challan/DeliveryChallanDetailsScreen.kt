package com.sucharu.sucharupro.ui.features.delivery.challan

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
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
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryChallanDetailsScreen(
    challanId: String,
    viewModel: DeliveryChallanDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(challanId) {
        viewModel.loadChallanDetails(challanId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.challan?.challanNo ?: "Challan Details") },
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
        } else if (uiState.challan == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Challan not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val challan = uiState.challan!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Overview Card
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
                                    text = challan.challanNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliveryChallanStatusBadge(status = challan.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Delivery Order Ref", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        uiState.deliveryOrder?.deliveryOrderNo ?: challan.deliveryOrderId.take(8),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Column {
                                    Text("Challan Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    DeliveryChallanTypeBadge(type = challan.challanType)
                                }
                                Column {
                                    Text("Issue Date", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(challan.issueDate)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val notes = challan.notes
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
                        when (challan.status) {
                            DeliveryChallanStatus.DRAFT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.submitChallan(challan.challanId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Submit")
                                    }
                                }
                            }
                            DeliveryChallanStatus.PENDING -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.approveChallan(challan.challanId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                            DeliveryChallanStatus.APPROVED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.markReadyForDispatch(challan.challanId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.LocalShipping, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ready for Dispatch")
                                    }
                                }
                            }
                            else -> {}
                        }

                        if (challan.status in listOf(DeliveryChallanStatus.DRAFT, DeliveryChallanStatus.PENDING, DeliveryChallanStatus.APPROVED, DeliveryChallanStatus.READY_FOR_DISPATCH) &&
                            currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.cancelChallan(challan.challanId, currentUserId, "User requested cancellation", currentUserRole) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Challan Lines Section
                item {
                    Text(
                        "Allocated Items (${uiState.lines.size})",
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
                                Text("Product: ${line.productId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("DO Line Ref: ${line.deliveryOrderLineId.take(8)}...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                val lineNotes = line.notes
                                if (lineNotes != null) {
                                    Text(lineNotes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Text(
                                "Qty: ${line.quantity}",
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
                        HorizontalDivider(modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}
