package com.sucharu.sucharupro.ui.features.returns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Return Inspection & Decision Management Screen (Module 11 Step 03).
 * Formatted with progressive dark ERP design system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnInspectionScreen(
    returnId: String,
    actorId: String,
    callerRole: UserRole? = null,
    callerProjectId: String? = null,
    viewModel: ReturnInspectionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReasonInput by remember { mutableStateOf("") }

    LaunchedEffect(returnId) {
        viewModel.loadInspection(returnId, callerRole, callerProjectId)
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = {
                Text(
                    text = "Reject Return Request",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFCA5A5)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Please provide the formal reason for rejecting this return request. This reason is required and will be logged in the audit trail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        placeholder = { Text("Enter rejection reason...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonInput.isNotBlank()) {
                            showRejectDialog = false
                            viewModel.rejectReturn(
                                rejectionReason = rejectionReasonInput,
                                actorId = actorId,
                                callerRole = callerRole,
                                callerProjectId = callerProjectId
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = rejectionReasonInput.isNotBlank()
                ) {
                    Text("Confirm Rejection", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inspection • ${uiState.returnRequest?.returnNo ?: "Return"}",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        } else if (uiState.returnRequest == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.errorMessage ?: "Return Request not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val req = uiState.returnRequest!!
            val isEditable = req.status == ReturnStatus.UNDER_INSPECTION

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success / Error Banners
                if (uiState.actionSuccessMessage != null) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.actionSuccessMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }
                }

                if (uiState.errorMessage != null) {
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                // Inspection Summary Header
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = req.returnNo,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Customer: ${req.customerId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            ReturnStatusBadge(status = req.status)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Declared Reason",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                ReturnReasonChip(reason = req.reason)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Inspector",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = uiState.inspection?.inspectorId ?: actorId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (uiState.inspection?.decision != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Final Decision",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                ReturnDecisionBadge(decision = uiState.inspection!!.decision!!)
                            }
                            if (!uiState.inspection?.decisionReason.isNullOrBlank()) {
                                Text(
                                    text = "Rejection Reason: ${uiState.inspection?.decisionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                }

                // QC Inspection Checklist
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "QC Inspection Checklist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        uiState.checklist.forEach { item ->
                            ChecklistRow(
                                item = item,
                                enabled = isEditable,
                                onToggle = { viewModel.toggleChecklistItem(item.itemId) }
                            )
                        }
                    }
                }

                // Item Quantities Verification
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Item Quantities Verification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        uiState.items.forEach { item ->
                            val accepted = uiState.itemAcceptedQuantities[item.returnItemId] ?: item.acceptedQuantity
                            val rejected = uiState.itemRejectedQuantities[item.returnItemId] ?: item.rejectedQuantity

                            InspectionItemVerificationRow(
                                item = item,
                                accepted = accepted,
                                rejected = rejected,
                                isEditable = isEditable,
                                onAcceptedChange = { viewModel.updateAcceptedQuantity(item.returnItemId, it) },
                                onRejectedChange = { viewModel.updateRejectedQuantity(item.returnItemId, it) }
                            )
                        }
                    }
                }

                // Findings & Notes
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Inspection Findings & Observations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        if (isEditable) {
                            OutlinedTextField(
                                value = uiState.findings,
                                onValueChange = { viewModel.updateFindings(it) },
                                placeholder = { Text("Enter physical defect observations, test results, or RMA notes...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        } else {
                            Text(
                                text = if (uiState.findings.isNotBlank()) uiState.findings else "No inspection findings recorded.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Decision Action Controls
                if (isEditable) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Approve Button
                        Button(
                            onClick = {
                                viewModel.approveReturn(
                                    actorId = actorId,
                                    callerRole = callerRole,
                                    callerProjectId = callerProjectId
                                )
                            },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Approve Return",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Reject Button
                        Button(
                            onClick = { showRejectDialog = true },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reject Return",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Save Draft Button
                        OutlinedButton(
                            onClick = {
                                viewModel.saveDraftInspection(
                                    actorId = actorId,
                                    callerRole = callerRole,
                                    callerProjectId = callerProjectId
                                )
                            },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Inspection Draft",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Audit Trail
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Audit Trail (${uiState.auditEvents.size} events)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        if (uiState.auditEvents.isEmpty()) {
                            Text(
                                text = "No audit events logged yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            uiState.auditEvents.forEach { event ->
                                AuditInspectionEventRow(event = event, dateFormat = dateFormat)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: InspectionChecklistItem,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isPassed,
            onCheckedChange = { if (enabled) onToggle() },
            enabled = enabled,
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF10B981),
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.isPassed) Color(0xFF6EE7B7) else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (item.isPassed) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun InspectionItemVerificationRow(
    item: ReturnItem,
    accepted: Int,
    rejected: Int,
    isEditable: Boolean,
    onAcceptedChange: (Int) -> Unit,
    onRejectedChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Product: ${item.productId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Requested: ${item.requestedQuantity} ${item.unit ?: "pcs"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accepted Quantity Control
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Accepted: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6EE7B7),
                    fontWeight = FontWeight.SemiBold
                )
                if (isEditable) {
                    IconButton(
                        onClick = { onAcceptedChange((accepted - 1).coerceAtLeast(0)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$accepted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                    IconButton(
                        onClick = { onAcceptedChange(accepted + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text(
                        text = "$accepted",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }
            }

            // Rejected Quantity Control
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Rejected: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.SemiBold
                )
                if (isEditable) {
                    IconButton(
                        onClick = { onRejectedChange((rejected - 1).coerceAtLeast(0)) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = "$rejected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                    IconButton(
                        onClick = { onRejectedChange(rejected + 1) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                } else {
                    Text(
                        text = "$rejected",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuditInspectionEventRow(
    event: ReturnActivityEvent,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.activityType.displayName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "By ${event.actorId} • ${dateFormat.format(Date(event.timestamp))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            val notes = event.notes
            if (!notes.isNullOrBlank()) {
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
