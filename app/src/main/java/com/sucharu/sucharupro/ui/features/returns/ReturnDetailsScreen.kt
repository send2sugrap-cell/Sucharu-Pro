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
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.ui.components.AppCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Return Request Details Screen (Module 11 Step 02 & Step 03).
 * Formatted with the progressive dark ERP aesthetic, inspection summary, and audit timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnDetailsScreen(
    returnId: String,
    actorId: String,
    callerRole: UserRole? = null,
    callerProjectId: String? = null,
    viewModel: ReturnDetailsViewModel,
    onBack: () -> Unit,
    onInspectClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(returnId) {
        viewModel.loadDetails(returnId, callerRole, callerProjectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.returnRequest?.returnNo ?: "Return Details",
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success / Error Feedback
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

                // Summary Header Card
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
                                    text = "ID: ${req.returnId}",
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
                                    text = "Customer ID",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = req.customerId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Return Reason",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                ReturnReasonChip(reason = req.reason)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Source Challan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = req.originalChallanId ?: "N/A",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Requested By",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = req.requestedBy,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        val desc = req.description
                        if (!desc.isNullOrBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Description / Notes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Inspection Summary Card (Step 03)
                if (uiState.inspection != null) {
                    val insp = uiState.inspection!!
                    AppCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Return Inspection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                val decision = insp.decision
                                if (decision != null) {
                                    ReturnDecisionBadge(decision = decision)
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Inspector",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = insp.inspectorId,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Status",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = insp.status.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFCD34D)
                                    )
                                }
                            }

                            val passedCount = insp.checklist.count { it.isPassed }
                            Text(
                                text = "Checklist: $passedCount / ${insp.checklist.size} passed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!insp.findings.isNullOrBlank()) {
                                Text(
                                    text = "Findings: ${insp.findings}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!insp.decisionReason.isNullOrBlank()) {
                                Text(
                                    text = "Decision Reason: ${insp.decisionReason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }
                }

                // Physical Receiving Summary (Module 11 Step 04)
                if (uiState.receivingInfo != null) {
                    ReturnReceivingSummaryCard(receivingInfo = uiState.receivingInfo!!)
                }

                // Inventory Reconciliation Summary (Module 11 Step 04)
                if (uiState.reconciliationResult != null) {
                    ReturnReconciliationSummaryCard(reconciliationResult = uiState.reconciliationResult!!)
                }

                // Settlement Summary (Module 11 Step 05)
                if (uiState.settlement != null) {
                    ReturnSettlementSummaryCard(settlement = uiState.settlement!!)
                }

                // Line Items Section
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Return Items (${uiState.items.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        if (uiState.items.isEmpty()) {
                            Text(
                                text = "No items attached to this return request.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            uiState.items.forEach { item ->
                                ReturnItemRow(item = item)
                            }
                        }
                    }
                }

                // Audit History Section
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
                                AuditEventRow(event = event, dateFormat = dateFormat)
                            }
                        }
                    }
                }

                // Action Controls & RBAC-Aware Workflows
                val canMutate = callerRole == null ||
                        callerRole == UserRole.ADMIN ||
                        callerRole == UserRole.MANAGER ||
                        callerRole == UserRole.WAREHOUSE

                if (req.status == ReturnStatus.REQUESTED) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                viewModel.submitForInspection(
                                    actorId = actorId,
                                    callerRole = callerRole,
                                    callerProjectId = callerProjectId
                                )
                            },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit for Inspection",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.cancelReturn(
                                    actorId = actorId,
                                    callerRole = callerRole,
                                    callerProjectId = callerProjectId
                                )
                            },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel Return Request",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (req.status == ReturnStatus.UNDER_INSPECTION) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (onInspectClick != null) {
                            Button(
                                onClick = { onInspectClick(req.returnId) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0284C7)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Assignment, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Perform Inspection & Decision",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF451A03))
                                .border(1.dp, Color(0xFFB45309), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "This return is currently UNDER INSPECTION. Standard editing is restricted.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCD34D)
                            )
                        }
                    }
                } else if (req.status == ReturnStatus.APPROVED) {
                    if (canMutate) {
                        Button(
                            onClick = { viewModel.openReceiveDialog() },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F766E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Receive Physical Return",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (req.status == ReturnStatus.RETURN_RECEIVED) {
                    if (canMutate) {
                        Button(
                            onClick = { viewModel.openReconcileDialog() },
                            enabled = !uiState.isSubmittingAction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6D28D9)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reconcile Inventory & Process Return",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else if (req.status == ReturnStatus.PROCESSED) {
                    if (uiState.settlement == null) {
                        val canSettle = callerRole == null ||
                                callerRole == UserRole.ADMIN ||
                                callerRole == UserRole.MANAGER ||
                                callerRole == UserRole.ACCOUNTS

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (canSettle) {
                                Button(
                                    onClick = { viewModel.setSettleDialogVisible(true) },
                                    enabled = !uiState.isSubmittingAction,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Settle Return & Financial Resolution",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2E1065))
                                    .border(1.dp, Color(0xFF6D28D9), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFC4B5FD),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Return Processed — Awaiting Settlement",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDDD6FE)
                                        )
                                        Text(
                                            text = "Inventory reconciliation is complete. Financial resolution / credit note / replacement can now be finalized.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFC4B5FD)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF064E3B))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Return Settled & Closed",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0)
                                    )
                                    Text(
                                        text = "Final commercial and financial disposition completed. Lifecycle finished.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Dialog Integrations
            if (uiState.showReceiveDialog) {
                val expectedApprovedQty = uiState.items.sumOf { it.acceptedQuantity }.let {
                    if (it > 0) it else uiState.items.sumOf { item -> item.requestedQuantity }
                }
                ReceiveReturnDialog(
                    expectedApprovedQty = expectedApprovedQty,
                    isSubmitting = uiState.isSubmittingAction,
                    onDismiss = { viewModel.closeReceiveDialog() },
                    onConfirm = { actual, accepted, rejected, damaged, remarks ->
                        viewModel.receiveReturn(
                            actualQty = actual,
                            acceptedQty = accepted,
                            rejectedQty = rejected,
                            damagedQty = damaged,
                            remarks = remarks,
                            actorId = actorId,
                            callerRole = callerRole,
                            callerProjectId = callerProjectId
                        )
                    }
                )
            }

            if (uiState.showReconcileDialog) {
                ReconcileInventoryDialog(
                    acceptedQty = uiState.receivingInfo?.acceptedQty ?: 0,
                    isSubmitting = uiState.isSubmittingAction,
                    onDismiss = { viewModel.closeReconcileDialog() },
                    onConfirm = { warehouseId, locationId ->
                        viewModel.reconcileInventoryAndProcess(
                            warehouseId = warehouseId,
                            locationId = locationId,
                            actorId = actorId,
                            callerRole = callerRole,
                            callerProjectId = callerProjectId
                        )
                    }
                )
            }

            if (uiState.showSettleDialog) {
                SettleReturnDialog(
                    isSubmitting = uiState.isSubmittingAction,
                    onDismiss = { viewModel.setSettleDialogVisible(false) },
                    onConfirm = { resolutionType, amount, creditNoteId, replacementOrderId, reworkId, notes ->
                        viewModel.settleReturn(
                            resolutionType = resolutionType,
                            amount = amount,
                            creditNoteId = creditNoteId,
                            replacementOrderId = replacementOrderId,
                            reworkId = reworkId,
                            notes = notes,
                            actorId = actorId,
                            callerRole = callerRole,
                            callerProjectId = callerProjectId
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ReturnItemRow(item: ReturnItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                text = "${item.requestedQuantity} ${item.unit ?: "pcs"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (item.acceptedQuantity > 0 || item.rejectedQuantity > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Accepted: ${item.acceptedQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6EE7B7),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Rejected: ${item.rejectedQuantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFCA5A5),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (item.originalChallanItemId != null) {
            Text(
                text = "Challan Item: ${item.originalChallanItemId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (!item.notes.isNullOrBlank()) {
            Text(
                text = "Notes: ${item.notes}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuditEventRow(
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
