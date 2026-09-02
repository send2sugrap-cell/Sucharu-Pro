package com.sucharu.sucharupro.ui.features.delivery.verification

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
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
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryItemVerificationDetailsScreen(
    verificationId: String,
    viewModel: DeliveryItemVerificationDetailsViewModel,
    currentUserRole: UserRole,
    currentUserId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    LaunchedEffect(verificationId) {
        viewModel.loadVerificationDetails(verificationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.verification?.verificationNo ?: "Delivery Verification") },
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
        } else if (uiState.verification == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Verification not found",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val verification = uiState.verification!!
            val summary = uiState.summary

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
                                    text = verification.verificationNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                DeliveryItemVerificationStatusBadge(status = verification.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Dispatch", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(verification.dispatchExecutionId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Challan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(verification.deliveryChallanId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Order", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                    Text(verification.deliveryOrderId.take(10), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            if (verification.verifiedBy != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Verified by ${verification.verifiedBy} at ${dateFormat.format(Date(verification.verifiedAt ?: 0))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                // Reconciliation Summary Card
                if (summary != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (summary.hasDiscrepancies) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (summary.hasDiscrepancies) "Reconciliation Summary (Discrepancies Found)" else "Reconciliation Summary (All Matched)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.hasDiscrepancies) Color(0xFFE65100) else Color(0xFF2E7D32)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Expected: ${summary.expectedTotalQuantity}", style = MaterialTheme.typography.bodyMedium)
                                    Text("Verified: ${summary.verifiedTotalQuantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Text("Shortage: ${summary.shortageTotalQuantity}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD32F2F))
                                    Text("Excess: ${summary.excessTotalQuantity}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0288D1))
                                }
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
                        when (verification.status) {
                            DeliveryItemVerificationStatus.DRAFT -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.submitVerification(verification.verificationId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Submit")
                                    }
                                }
                            }
                            DeliveryItemVerificationStatus.PENDING -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.startVerification(verification.verificationId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Start Verification")
                                    }
                                }
                            }
                            DeliveryItemVerificationStatus.IN_PROGRESS -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.WAREHOUSE)) {
                                    Button(
                                        onClick = { viewModel.completeVerification(verification.verificationId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Complete Verification")
                                    }
                                }
                            }
                            DeliveryItemVerificationStatus.VERIFIED -> {
                                if (currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                                    Button(
                                        onClick = { viewModel.closeVerification(verification.verificationId, currentUserId, currentUserRole) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4527A0))
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Close Verification")
                                    }
                                }
                            }
                            else -> {}
                        }

                        if (!verification.status.isTerminal && currentUserRole in listOf(UserRole.ADMIN, UserRole.MANAGER)) {
                            OutlinedButton(
                                onClick = { viewModel.cancelVerification(verification.verificationId, currentUserId, "User requested cancellation", currentUserRole) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }

                // Lines Section
                item {
                    Text(
                        "Verified Delivery Items (${uiState.lines.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.lines) { line ->
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
                                Text("Product: ${line.productId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                DeliveryItemVerificationResultBadge(resultType = line.resultType)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected: ${line.expectedQuantity} • Verified: ${line.verifiedQuantity}", style = MaterialTheme.typography.bodySmall)
                                if (line.issueQuantity > 0.0) {
                                    Text("Discrepancy Qty: ${line.issueQuantity}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                                }
                            }

                            if (line.batchId != null || line.lotId != null) {
                                Text("Batch: ${line.batchId ?: "N/A"} • Lot: ${line.lotId ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }

                            DeliveryItemVerificationIssueBadge(issueType = line.issueType)
                        }
                    }
                }

                // Audit History Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Verification Audit Trail",
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
