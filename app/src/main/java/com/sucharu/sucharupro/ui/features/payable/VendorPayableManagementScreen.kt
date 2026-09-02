package com.sucharu.sucharupro.ui.features.payable

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

// Design System Palette (Dark Navy / Near-Black Cyber-ERP)
private val DeepNavyBg = Color(0xFF0B111E)
private val CardSurface = Color(0xFF141E33)
private val CardBorder = Color(0xFF223254)
private val AccentCyan = Color(0xFF00E5FF)
private val AccentEmerald = Color(0xFF00E676)
private val AccentAmber = Color(0xFFFFB300)
private val AccentRose = Color(0xFFFF1744)
private val AccentPurple = Color(0xFFD500F9)
private val TextPrimary = Color(0xFFF0F4FC)
private val TextSecondary = Color(0xFF90A4AE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPayableManagementScreen(
    payables: List<VendorPayableDto> = emptyList(),
    summary: VendorPayableSummaryDto? = null,
    userRole: String = "ADMIN",
    onCreatePayable: (CreateVendorPayableRequest) -> Unit = {},
    onUpdateDraft: (String, UpdateVendorPayableRequest) -> Unit = { _, _ -> },
    onSubmitPayable: (String) -> Unit = {},
    onApprovePayable: (String, String?) -> Unit = { _, _ -> },
    onRejectPayable: (String, String) -> Unit = { _, _ -> },
    onCancelPayable: (String, String) -> Unit = { _, _ -> },
    onVoidPayable: (String, String) -> Unit = { _, _ -> },
    onAllocatePayment: (String, AllocateVendorPayablePaymentRequest) -> Unit = { _, _ -> },
    onViewAudit: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showCreateModal by remember { mutableStateOf(false) }
    var editingPayable by remember { mutableStateOf<VendorPayableDto?>(null) }
    var allocatingPayable by remember { mutableStateOf<VendorPayableDto?>(null) }

    var actionDialogType by remember { mutableStateOf<String?>(null) } // "APPROVE", "REJECT", "CANCEL", "VOID"
    var actionTargetPayable by remember { mutableStateOf<VendorPayableDto?>(null) }
    var actionReasonOrNote by remember { mutableStateOf("") }

    val filteredList = remember(payables, searchQuery, selectedFilter) {
        payables.filter { p ->
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "OVERDUE" -> p.isOverdue
                else -> p.status.equals(selectedFilter, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                    p.payableNumber.contains(searchQuery, ignoreCase = true) ||
                    p.vendorId.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true) ||
                    (p.billReference?.contains(searchQuery, ignoreCase = true) == true)
            matchesFilter && matchesSearch
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavyBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vendor Payables & Supplier Liabilities",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Module 15 → Step 02: Commercial Printing Liability Ledger",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                if (userRole in setOf("ADMIN", "MANAGER", "STAFF")) {
                    Button(
                        onClick = { showCreateModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Record Payable", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Summary KPI Cards
            if (summary != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PayableKpiCard("Approved Total", "${summary.totalApprovedLiability} ${summary.currency}", AccentCyan, Modifier.weight(1f))
                    PayableKpiCard("Total Paid", "${summary.totalPaid} ${summary.currency}", AccentEmerald, Modifier.weight(1f))
                    PayableKpiCard("Outstanding", "${summary.totalOutstanding} ${summary.currency}", AccentAmber, Modifier.weight(1f))
                    PayableKpiCard("Overdue", "${summary.totalOverdue} ${summary.currency}", AccentRose, Modifier.weight(1f))
                }
            }

            // Search Bar & Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by bill, vendor, or description...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedContainerColor = CardSurface,
                        unfocusedContainerColor = CardSurface
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "DRAFT", "SUBMITTED", "APPROVED", "PARTIALLY_PAID", "PAID", "OVERDUE").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan.copy(alpha = 0.2f),
                            selectedLabelColor = AccentCyan,
                            containerColor = CardSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedFilter == filter,
                            borderColor = if (selectedFilter == filter) AccentCyan else CardBorder
                        )
                    )
                }
            }

            // List of Payables
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.payableId }) { payable ->
                    VendorPayableCard(
                        payable = payable,
                        userRole = userRole,
                        onEdit = { editingPayable = payable },
                        onSubmit = { onSubmitPayable(payable.payableId) },
                        onApprove = {
                            actionTargetPayable = payable
                            actionDialogType = "APPROVE"
                            actionReasonOrNote = ""
                        },
                        onReject = {
                            actionTargetPayable = payable
                            actionDialogType = "REJECT"
                            actionReasonOrNote = ""
                        },
                        onCancel = {
                            actionTargetPayable = payable
                            actionDialogType = "CANCEL"
                            actionReasonOrNote = ""
                        },
                        onVoid = {
                            actionTargetPayable = payable
                            actionDialogType = "VOID"
                            actionReasonOrNote = ""
                        },
                        onAllocatePayment = { allocatingPayable = payable },
                        onViewAudit = { onViewAudit(payable.payableId) }
                    )
                }
            }
        }

        // Modals & Dialogs
        if (showCreateModal) {
            VendorPayableFormDialog(
                title = "Record New Vendor Payable",
                initialVendorId = "",
                initialAmount = "",
                initialDesc = "",
                onDismiss = { showCreateModal = false },
                onSave = { vendorId, amount, terms, customDays, desc, billRef, autoSubmit ->
                    onCreatePayable(
                        CreateVendorPayableRequest(
                            vendorId = vendorId,
                            originalAmount = amount,
                            paymentTerms = terms,
                            customTermDays = customDays,
                            description = desc,
                            billReference = billRef,
                            autoSubmit = autoSubmit
                        )
                    )
                    showCreateModal = false
                }
            )
        }

        editingPayable?.let { payable ->
            VendorPayableFormDialog(
                title = "Edit Draft Payable (${payable.payableNumber})",
                initialVendorId = payable.vendorId,
                initialAmount = payable.originalAmount,
                initialDesc = payable.description,
                initialBillRef = payable.billReference ?: "",
                initialTerms = payable.paymentTerms,
                onDismiss = { editingPayable = null },
                onSave = { vendorId, amount, terms, customDays, desc, billRef, _ ->
                    onUpdateDraft(
                        payable.payableId,
                        UpdateVendorPayableRequest(
                            vendorId = vendorId,
                            originalAmount = amount,
                            paymentTerms = terms,
                            customTermDays = customDays,
                            description = desc,
                            billReference = billRef
                        )
                    )
                    editingPayable = null
                }
            )
        }

        allocatingPayable?.let { payable ->
            AllocatePaymentDialog(
                payable = payable,
                onDismiss = { allocatingPayable = null },
                onAllocate = { amount, method, ref, notes ->
                    onAllocatePayment(
                        payable.payableId,
                        AllocateVendorPayablePaymentRequest(
                            amount = amount,
                            paymentMethod = method,
                            paymentReference = ref,
                            notes = notes
                        )
                    )
                    allocatingPayable = null
                }
            )
        }

        // Status Mutation Reason / Note Dialog
        if (actionDialogType != null && actionTargetPayable != null) {
            val payable = actionTargetPayable!!
            val action = actionDialogType!!
            AlertDialog(
                onDismissRequest = {
                    actionDialogType = null
                    actionTargetPayable = null
                },
                title = { Text("$action Payable: ${payable.payableNumber}", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (action == "APPROVE") "Enter optional approval notes:" else "Enter mandatory $action rationale:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = actionReasonOrNote,
                            onValueChange = { actionReasonOrNote = it },
                            placeholder = { Text(if (action == "APPROVE") "e.g. Verified with work order" else "Reason required...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedContainerColor = DeepNavyBg,
                                unfocusedContainerColor = DeepNavyBg
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            when (action) {
                                "APPROVE" -> onApprovePayable(payable.payableId, actionReasonOrNote.ifBlank { null })
                                "REJECT" -> onRejectPayable(payable.payableId, actionReasonOrNote)
                                "CANCEL" -> onCancelPayable(payable.payableId, actionReasonOrNote)
                                "VOID" -> onVoidPayable(payable.payableId, actionReasonOrNote)
                            }
                            actionDialogType = null
                            actionTargetPayable = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (action) {
                                "APPROVE" -> AccentEmerald
                                "VOID" -> AccentPurple
                                else -> AccentRose
                            }
                        ),
                        enabled = action == "APPROVE" || actionReasonOrNote.isNotBlank()
                    ) {
                        Text("Confirm $action", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        actionDialogType = null
                        actionTargetPayable = null
                    }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardSurface
            )
        }
    }
}

@Composable
fun PayableKpiCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun VendorPayableCard(
    payable: VendorPayableDto,
    userRole: String,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onVoid: () -> Unit,
    onAllocatePayment: () -> Unit,
    onViewAudit: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val isManagerOrAdmin = userRole in setOf("ADMIN", "MANAGER")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (payable.isOverdue) AccentRose.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row 1: Number, Vendor, Status & Aging
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = payable.payableNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• ${payable.vendorId}",
                        fontSize = 13.sp,
                        color = AccentCyan
                    )
                    if (payable.billReference != null) {
                        Text(
                            text = "(Ref: ${payable.billReference})",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (payable.isOverdue) {
                        StatusChip(text = "OVERDUE", color = AccentRose)
                    }
                    StatusChip(
                        text = payable.status,
                        color = when (payable.status) {
                            "DRAFT" -> Color.Gray
                            "SUBMITTED" -> AccentAmber
                            "APPROVED" -> AccentCyan
                            "PARTIALLY_PAID" -> AccentPurple
                            "PAID" -> AccentEmerald
                            "VOIDED" -> Color.Magenta
                            else -> AccentRose
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Description
            Text(
                text = payable.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Financial Details & Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Original: ${payable.originalAmount} ${payable.currency}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Outstanding: ${payable.outstandingAmount} ${payable.currency}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (payable.outstandingAmount == "0.0000") AccentEmerald else AccentAmber
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Terms: ${payable.paymentTerms}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "Due: ${dateFormat.format(Date(payable.dueDate))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (payable.isOverdue) AccentRose else TextPrimary
                    )
                }
            }

            // Action Buttons Row
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CardBorder.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onViewAudit) {
                    Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Audit", color = TextSecondary, fontSize = 12.sp)
                }

                if (payable.status in setOf("DRAFT", "REJECTED")) {
                    TextButton(onClick = onEdit) {
                        Text("Edit", color = AccentCyan, fontSize = 12.sp)
                    }
                    TextButton(onClick = onSubmit) {
                        Text("Submit", color = AccentAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (payable.status == "SUBMITTED" && isManagerOrAdmin) {
                    TextButton(onClick = onReject) {
                        Text("Reject", color = AccentRose, fontSize = 12.sp)
                    }
                    TextButton(onClick = onApprove) {
                        Text("Approve", color = AccentEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (payable.status in setOf("APPROVED", "PARTIALLY_PAID") && isManagerOrAdmin) {
                    TextButton(onClick = onVoid) {
                        Text("Void", color = AccentRose, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onAllocatePayment,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Pay", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (payable.status in setOf("DRAFT", "SUBMITTED")) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = AccentRose, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPayableFormDialog(
    title: String,
    initialVendorId: String,
    initialAmount: String,
    initialDesc: String,
    initialBillRef: String = "",
    initialTerms: String = "NET_30",
    onDismiss: () -> Unit,
    onSave: (vendorId: String, amount: String, terms: String, customDays: Int?, desc: String, billRef: String?, autoSubmit: Boolean) -> Unit
) {
    var vendorId by remember { mutableStateOf(initialVendorId) }
    var amount by remember { mutableStateOf(initialAmount) }
    var description by remember { mutableStateOf(initialDesc) }
    var billRef by remember { mutableStateOf(initialBillRef) }
    var terms by remember { mutableStateOf(initialTerms) }
    var customDays by remember { mutableStateOf("") }
    var autoSubmit by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = vendorId,
                    onValueChange = { vendorId = it },
                    label = { Text("Vendor ID (e.g. VEND-1001)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = billRef,
                    onValueChange = { billRef = it },
                    label = { Text("Vendor Bill Reference (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = autoSubmit, onCheckedChange = { autoSubmit = it })
                    Text("Auto-submit for approval immediately", color = TextSecondary, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        vendorId,
                        amount,
                        terms,
                        customDays.toIntOrNull(),
                        description,
                        billRef.ifBlank { null },
                        autoSubmit
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                enabled = vendorId.isNotBlank() && amount.isNotBlank() && description.isNotBlank()
            ) {
                Text("Save Payable", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}

@Composable
fun AllocatePaymentDialog(
    payable: VendorPayableDto,
    onDismiss: () -> Unit,
    onAllocate: (amount: String, method: String, reference: String?, notes: String?) -> Unit
) {
    var amount by remember { mutableStateOf(payable.outstandingAmount) }
    var method by remember { mutableStateOf("BANK") }
    var reference by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val enteredAmountBigDecimal = amount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val outstandingBigDecimal = payable.outstandingAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val isOverAllocated = enteredAmountBigDecimal > outstandingBigDecimal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allocate Payment: ${payable.payableNumber}", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Outstanding Balance: ${payable.outstandingAmount} ${payable.currency}",
                    color = AccentAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Payment Amount (${payable.currency})") },
                    isError = isOverAllocated,
                    supportingText = if (isOverAllocated) {
                        { Text("Amount exceeds outstanding balance!", color = AccentRose) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Payment Ref (Cheque # / Trx ID)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAllocate(amount, method, reference.ifBlank { null }, notes.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                enabled = !isOverAllocated && enteredAmountBigDecimal > BigDecimal.ZERO
            ) {
                Text("Confirm Allocation", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        },
        containerColor = CardSurface
    )
}
