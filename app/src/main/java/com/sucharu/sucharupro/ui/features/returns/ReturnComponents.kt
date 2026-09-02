package com.sucharu.sucharupro.ui.features.returns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.StatusColor
import com.sucharu.sucharupro.ui.theme.spacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Visual design tokens for Return statuses (Module 11 Step 02 & Step 03 & Step 04).
 * Implements the progressive design system with high-contrast, premium dark tokens.
 */
object ReturnStatusTheme {
    val requested = StatusColor(
        container = Color(0xFF0C4A6E),
        content = Color(0xFF7DD3FC),
        border = Color(0xFF0284C7)
    )
    val underInspection = StatusColor(
        container = Color(0xFF451A03),
        content = Color(0xFFFCD34D),
        border = Color(0xFFB45309)
    )
    val approved = StatusColor(
        container = Color(0xFF064E3B),
        content = Color(0xFF6EE7B7),
        border = Color(0xFF047857)
    )
    val rejected = StatusColor(
        container = Color(0xFF450A0A),
        content = Color(0xFFFCA5A5),
        border = Color(0xFFB91C1C)
    )
    val returnReceived = StatusColor(
        container = Color(0xFF134E4A),
        content = Color(0xFF5EEAD4),
        border = Color(0xFF0F766E)
    )
    val processed = StatusColor(
        container = Color(0xFF2E1065),
        content = Color(0xFFC4B5FD),
        border = Color(0xFF6D28D9)
    )
    val cancelled = StatusColor(
        container = Color(0xFF1E293B),
        content = Color(0xFF94A3B8),
        border = Color(0xFF475569)
    )

    fun colorFor(status: ReturnStatus): StatusColor = when (status) {
        ReturnStatus.REQUESTED -> requested
        ReturnStatus.UNDER_INSPECTION -> underInspection
        ReturnStatus.APPROVED -> approved
        ReturnStatus.REJECTED -> rejected
        ReturnStatus.RETURN_RECEIVED -> returnReceived
        ReturnStatus.PROCESSED -> processed
        ReturnStatus.CANCELLED -> cancelled
    }
}

/**
 * Specialized Status Badge for Return Requests.
 */
@Composable
fun ReturnStatusBadge(
    status: ReturnStatus,
    modifier: Modifier = Modifier
) {
    val colors = ReturnStatusTheme.colorFor(status)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .border(width = 1.dp, color = colors.border, shape = shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(colors.content)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = status.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.content
        )
    }
}

/**
 * Specialized Decision Badge for Return Inspection Outcomes.
 */
@Composable
fun ReturnDecisionBadge(
    decision: ReturnDecision,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (decision) {
        ReturnDecision.APPROVE -> Triple(Color(0xFF064E3B), Color(0xFF6EE7B7), Color(0xFF047857))
        ReturnDecision.REJECT -> Triple(Color(0xFF450A0A), Color(0xFFFCA5A5), Color(0xFFB91C1C))
    }
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = decision.displayName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Chip component for ReturnReason.
 */
@Composable
fun ReturnReasonChip(
    reason: ReturnReason,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = reason.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Summary card for Return Request items in the list view.
 */
@Composable
fun ReturnSummaryCard(
    returnRequest: ReturnRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(returnRequest.requestedAt))

    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = returnRequest.returnNo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                ReturnStatusBadge(status = returnRequest.status)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Customer: ${returnRequest.customerId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (returnRequest.originalChallanId != null) {
                        Text(
                            text = "Challan: ${returnRequest.originalChallanId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ReturnReasonChip(reason = returnRequest.reason)
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Requested: $formattedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Summary card displaying the physical receiving details for a return request (Module 11 Step 04).
 */
@Composable
fun ReturnReceivingSummaryCard(
    receivingInfo: ReturnReceivingInfo,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(receivingInfo.receivedAt))

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Physical Receiving",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5EEAD4)
                )
                ReturnStatusBadge(status = ReturnStatus.RETURN_RECEIVED)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Actual Qty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${receivingInfo.actualQty} pcs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Accepted Qty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${receivingInfo.acceptedQty} pcs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Rejected Qty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${receivingInfo.rejectedQty} pcs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCA5A5)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Damaged Qty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${receivingInfo.damagedQty} pcs",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFCD34D)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Received By",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = receivingInfo.receiverId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Received At",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (!receivingInfo.condition.isNullOrBlank()) {
                Text(
                    text = "Condition: ${receivingInfo.condition}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!receivingInfo.packaging.isNullOrBlank()) {
                Text(
                    text = "Packaging: ${receivingInfo.packaging}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!receivingInfo.damageNotes.isNullOrBlank()) {
                Text(
                    text = "Notes / Remarks: ${receivingInfo.damageNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Summary card displaying the inventory reconciliation outcome for a return request (Module 11 Step 04).
 */
@Composable
fun ReturnReconciliationSummaryCard(
    reconciliationResult: ReturnReconciliationResult,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(reconciliationResult.completedAt))

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory Reconciliation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC4B5FD)
                )
                ReturnStatusBadge(status = ReturnStatus.PROCESSED)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Stocked-In Quantity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${reconciliationResult.acceptedQty} pcs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6EE7B7)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Reconciled By",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = reconciliationResult.reconciledBy,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stock-In Record ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = reconciliationResult.stockInRecordId ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ledger Entry ID",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = reconciliationResult.ledgerEntryId ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Completed At",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * Modal Dialog for physically receiving an APPROVED return request (Module 11 Step 04).
 */
@Composable
fun ReceiveReturnDialog(
    expectedApprovedQty: Int,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (actualQty: Int, acceptedQty: Int, rejectedQty: Int, damagedQty: Int, remarks: String?) -> Unit
) {
    var actualQtyText by remember { mutableStateOf(if (expectedApprovedQty > 0) expectedApprovedQty.toString() else "") }
    var acceptedQtyText by remember { mutableStateOf(if (expectedApprovedQty > 0) expectedApprovedQty.toString() else "") }
    var rejectedQtyText by remember { mutableStateOf("0") }
    var damagedQtyText by remember { mutableStateOf("0") }
    var remarksText by remember { mutableStateOf("") }

    val actual = actualQtyText.toIntOrNull() ?: 0
    val accepted = acceptedQtyText.toIntOrNull() ?: 0
    val rejected = rejectedQtyText.toIntOrNull() ?: 0
    val damaged = damagedQtyText.toIntOrNull() ?: 0

    val sum = accepted + rejected + damaged
    val isBalanced = actual > 0 && sum == actual
    val isValid = isBalanced && accepted >= 0 && rejected >= 0 && damaged >= 0

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Receive Physical Return",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF5EEAD4)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Verify the physical quantities received at the warehouse.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = actualQtyText,
                    onValueChange = { actualQtyText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Actual Quantity Received") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = acceptedQtyText,
                        onValueChange = { acceptedQtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Accepted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rejectedQtyText,
                        onValueChange = { rejectedQtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Rejected") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = damagedQtyText,
                        onValueChange = { damagedQtyText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Damaged") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live Mathematical Balance Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isValid) Color(0xFF134E4A) else Color(0xFF450A0A))
                        .border(
                            width = 1.dp,
                            color = if (isValid) Color(0xFF0F766E) else Color(0xFFB91C1C),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Balance: Actual ($actual) = Accepted ($accepted) + Rejected ($rejected) + Damaged ($damaged)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isValid) Color(0xFF5EEAD4) else Color(0xFFFCA5A5)
                        )
                        if (!isValid) {
                            Text(
                                text = if (actual <= 0) {
                                    "Actual quantity must be greater than 0."
                                } else {
                                    "Quantities do not balance: sum is $sum but actual is $actual."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFFF87171)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = remarksText,
                    onValueChange = { remarksText = it },
                    label = { Text("Remarks / Condition Notes (Optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        onConfirm(actual, accepted, rejected, damaged, remarksText.ifBlank { null })
                    }
                },
                enabled = isValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F766E)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Confirm Physical Receipt")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Modal Dialog for reconciling inventory and processing return (Module 11 Step 04).
 */
@Composable
fun ReconcileInventoryDialog(
    acceptedQty: Int,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (warehouseId: String, locationId: String) -> Unit
) {
    var warehouseIdText by remember { mutableStateOf("WH-MAIN") }
    var locationIdText by remember { mutableStateOf("LOC-RETURN-01") }

    val isValid = warehouseIdText.isNotBlank() && locationIdText.isNotBlank()

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Reconcile Inventory & Process Return",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFC4B5FD)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E1065))
                        .border(1.dp, Color(0xFF6D28D9), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Reconciliation will stock in exactly $acceptedQty accepted units into finished-product inventory and create the canonical Stock-In and ledger records.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFDDD6FE)
                    )
                }

                OutlinedTextField(
                    value = warehouseIdText,
                    onValueChange = { warehouseIdText = it },
                    label = { Text("Warehouse ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = locationIdText,
                    onValueChange = { locationIdText = it },
                    label = { Text("Location / Bin ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValid && !isSubmitting) {
                        onConfirm(warehouseIdText.trim(), locationIdText.trim())
                    }
                },
                enabled = isValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6D28D9)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Confirm Stock-In & Process")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Summary card for Customer Return Financial Settlement (Module 11 Step 05).
 */
@Composable
fun ReturnSettlementSummaryCard(
    settlement: ReturnSettlement,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AppCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Settled",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Settlement & Resolution",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF10B981)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF064E3B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = settlement.resolutionType.displayName.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF34D399)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Settlement Amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = settlement.amount.formatted(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Settled Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (settlement.settledAt > 0) dateFormat.format(Date(settlement.settledAt)) else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            settlement.creditNoteId?.let { cnId ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Credit Note ID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = cnId,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF60A5FA)
                    )
                }
            }

            settlement.replacementOrderId?.let { repId ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Replacement Order ID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = repId,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA78BFA)
                    )
                }
            }

            settlement.reworkId?.let { rewId ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rework Job ID:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = rewId,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            settlement.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Notes: $notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Modal Dialog for financial and commercial settlement of a return (Module 11 Step 05).
 */
@Composable
fun SettleReturnDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        resolutionType: ReturnResolutionType,
        amount: Money,
        creditNoteId: String?,
        replacementOrderId: String?,
        reworkId: String?,
        notes: String?
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(ReturnResolutionType.CREDIT_NOTE) }
    var amountText by remember { mutableStateOf("") }
    var referenceText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    val amountDouble = amountText.toDoubleOrNull() ?: 0.0
    val isAmountValid = amountDouble >= 0.0

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = "Settle Customer Return",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF34D399)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Select Resolution Type:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ReturnResolutionType.CREDIT_NOTE,
                        ReturnResolutionType.REFUND,
                        ReturnResolutionType.REPLACEMENT
                    ).forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        ReturnResolutionType.REWORK,
                        ReturnResolutionType.SCRAP_WRITE_OFF
                    ).forEach { type ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Settlement Amount (e.g. 500.00)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val refLabel = when (selectedType) {
                    ReturnResolutionType.CREDIT_NOTE -> "Credit Note ID (optional)"
                    ReturnResolutionType.REPLACEMENT -> "Replacement Order ID (optional)"
                    ReturnResolutionType.REWORK -> "Rework Job ID (optional)"
                    else -> "Reference ID (optional)"
                }

                OutlinedTextField(
                    value = referenceText,
                    onValueChange = { referenceText = it },
                    label = { Text(refLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Settlement Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isAmountValid && !isSubmitting) {
                        val amount = Money(amountDouble)
                        val ref = referenceText.trim().ifBlank { null }
                        val cnId = if (selectedType == ReturnResolutionType.CREDIT_NOTE) ref else null
                        val repId = if (selectedType == ReturnResolutionType.REPLACEMENT) ref else null
                        val rewId = if (selectedType == ReturnResolutionType.REWORK) ref else null
                        val notes = notesText.trim().ifBlank { null }

                        onConfirm(selectedType, amount, cnId, repId, rewId, notes)
                    }
                },
                enabled = isAmountValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981)
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Confirm Settlement")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}


