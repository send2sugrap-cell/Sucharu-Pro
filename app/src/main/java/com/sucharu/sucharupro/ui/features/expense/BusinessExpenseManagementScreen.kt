package com.sucharu.sucharupro.ui.features.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.BusinessExpenseAuditEventDto
import com.sucharu.sucharupro.data.api.model.BusinessExpenseCategoryDto
import com.sucharu.sucharupro.data.api.model.BusinessExpenseDto
import com.sucharu.sucharupro.data.api.model.CreateBusinessExpenseRequest
import com.sucharu.sucharupro.data.api.model.UpdateBusinessExpenseRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessExpenseManagementScreen(
    expenses: List<BusinessExpenseDto>,
    categories: List<BusinessExpenseCategoryDto>,
    onCreateExpense: (CreateBusinessExpenseRequest) -> Unit,
    onUpdateExpense: (String, UpdateBusinessExpenseRequest) -> Unit,
    onSubmitExpense: (String) -> Unit,
    onApproveExpense: (String, String?) -> Unit,
    onRejectExpense: (String, String) -> Unit,
    onCancelExpense: (String, String) -> Unit,
    onViewAuditTrail: (String) -> Unit,
    auditTrail: List<BusinessExpenseAuditEventDto>? = null,
    canApprove: Boolean = true,
    isStaff: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<BusinessExpenseDto?>(null) }
    var approvingExpenseId by remember { mutableStateOf<String?>(null) }
    var approvalNote by remember { mutableStateOf("") }
    var rejectingExpenseId by remember { mutableStateOf<String?>(null) }
    var rejectionReason by remember { mutableStateOf("") }
    var cancellingExpenseId by remember { mutableStateOf<String?>(null) }
    var cancellationReason by remember { mutableStateOf("") }
    var viewingAuditExpenseId by remember { mutableStateOf<String?>(null) }

    // KPI Aggregations (Operational projections only)
    val totalExpensesCount = expenses.size
    val totalAmount = expenses.mapNotNull { it.amount.toDoubleOrNull() }.sum()
    val draftCount = expenses.count { it.status == "DRAFT" }
    val pendingCount = expenses.count { it.status == "SUBMITTED" }
    val approvedCount = expenses.count { it.status == "APPROVED" || it.status == "POSTABLE" }
    val rejectedCount = expenses.count { it.status == "REJECTED" }
    val cancelledCount = expenses.count { it.status == "CANCELLED" }

    val filteredExpenses = expenses.filter { exp ->
        val matchesStatus = when (selectedFilter) {
            "DRAFT" -> exp.status == "DRAFT"
            "PENDING" -> exp.status == "SUBMITTED"
            "APPROVED" -> exp.status == "APPROVED" || exp.status == "POSTABLE"
            "REJECTED" -> exp.status == "REJECTED"
            "CANCELLED" -> exp.status == "CANCELLED"
            else -> true
        }
        val matchesCategory = selectedCategoryFilter == null || exp.expenseCategoryId == selectedCategoryFilter
        val matchesSearch = searchQuery.isBlank() ||
                exp.expenseNumber.contains(searchQuery, ignoreCase = true) ||
                exp.description.contains(searchQuery, ignoreCase = true) ||
                (exp.vendorId?.contains(searchQuery, ignoreCase = true) == true) ||
                (exp.jobId?.contains(searchQuery, ignoreCase = true) == true) ||
                exp.createdBy.contains(searchQuery, ignoreCase = true)

        matchesStatus && matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Business Expense Management", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Module 15 — Expense & Vendor Operations", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Record Expense", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Expense", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // --- KPI Summary Cards ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Total Recorded",
                    value = String.format(Locale.US, "৳%.2f", totalAmount),
                    subtitle = "$totalExpensesCount entries",
                    accentColor = Color(0xFF60A5FA),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Pending Approval",
                    value = pendingCount.toString(),
                    subtitle = "Needs Review",
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Approved",
                    value = approvedCount.toString(),
                    subtitle = "Postable to Ledger",
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Draft",
                    value = draftCount.toString(),
                    subtitle = "In Preparation",
                    accentColor = Color(0xFF94A3B8),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Rejected",
                    value = rejectedCount.toString(),
                    subtitle = "Needs Revision",
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }

            // --- Search & Filter Bar ---
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by expense #, description, vendor, job...", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            ),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("ALL", "DRAFT", "PENDING", "APPROVED", "REJECTED", "CANCELLED").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF3B82F6),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            // --- Expense Items List ---
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No business expenses match the criteria.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredExpenses, key = { it.expenseId }) { exp ->
                        ExpenseCard(
                            expense = exp,
                            category = categories.find { it.categoryId == exp.expenseCategoryId },
                            canApprove = canApprove,
                            onSubmit = { onSubmitExpense(exp.expenseId) },
                            onEdit = { editingExpense = exp },
                            onApprove = { approvingExpenseId = exp.expenseId },
                            onReject = { rejectingExpenseId = exp.expenseId },
                            onCancel = { cancellingExpenseId = exp.expenseId },
                            onViewAudit = {
                                viewingAuditExpenseId = exp.expenseId
                                onViewAuditTrail(exp.expenseId)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Create / Edit Dialog ---
    if (showCreateDialog || editingExpense != null) {
        ExpenseFormDialog(
            existing = editingExpense,
            categories = categories,
            onDismiss = {
                showCreateDialog = false
                editingExpense = null
            },
            onSave = { req, isEdit ->
                if (isEdit && editingExpense != null) {
                    onUpdateExpense(
                        editingExpense!!.expenseId,
                        UpdateBusinessExpenseRequest(
                            categoryId = req.categoryId,
                            amount = req.amount,
                            currency = req.currency,
                            expenseDate = req.expenseDate,
                            paymentMethod = req.paymentMethod,
                            paymentReference = req.paymentReference,
                            vendorId = req.vendorId,
                            jobId = req.jobId,
                            branchId = req.branchId,
                            locationId = req.locationId,
                            description = req.description,
                            notes = req.notes,
                            attachmentUrl = req.attachmentUrl,
                            attachmentMetadata = req.attachmentMetadata
                        )
                    )
                } else {
                    onCreateExpense(req)
                }
                showCreateDialog = false
                editingExpense = null
            }
        )
    }

    // --- Approve Modal ---
    if (approvingExpenseId != null) {
        AlertDialog(
            onDismissRequest = { approvingExpenseId = null },
            title = { Text("Approve Business Expense") },
            text = {
                Column {
                    Text("Are you sure you want to approve this expense? Once approved, it will be marked as postable to the Business Ledger.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = approvalNote,
                        onValueChange = { approvalNote = it },
                        label = { Text("Approval Note (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApproveExpense(approvingExpenseId!!, approvalNote.takeIf { it.isNotBlank() })
                        approvingExpenseId = null
                        approvalNote = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { approvingExpenseId = null }) { Text("Cancel") }
            }
        )
    }

    // --- Reject Modal ---
    if (rejectingExpenseId != null) {
        AlertDialog(
            onDismissRequest = { rejectingExpenseId = null },
            title = { Text("Reject Business Expense") },
            text = {
                Column {
                    Text("Please specify the reason for rejecting this expense. It will be returned to editable state for the creator.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Rejection Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReason.isNotBlank()) {
                            onRejectExpense(rejectingExpenseId!!, rejectionReason)
                            rejectingExpenseId = null
                            rejectionReason = ""
                        }
                    },
                    enabled = rejectionReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reject")
                }
            },
            dismissButton = {
                TextButton(onClick = { rejectingExpenseId = null }) { Text("Dismiss") }
            }
        )
    }

    // --- Cancel Modal ---
    if (cancellingExpenseId != null) {
        AlertDialog(
            onDismissRequest = { cancellingExpenseId = null },
            title = { Text("Cancel Business Expense") },
            text = {
                Column {
                    Text("Are you sure you want to cancel this expense? Cancelled expenses cannot be reactivated.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancellationReason,
                        onValueChange = { cancellationReason = it },
                        label = { Text("Cancellation Reason *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancellationReason.isNotBlank()) {
                            onCancelExpense(cancellingExpenseId!!, cancellationReason)
                            cancellingExpenseId = null
                            cancellationReason = ""
                        }
                    },
                    enabled = cancellationReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Cancel Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { cancellingExpenseId = null }) { Text("Dismiss") }
            }
        )
    }

    // --- Audit Trail Dialog ---
    if (viewingAuditExpenseId != null) {
        AlertDialog(
            onDismissRequest = { viewingAuditExpenseId = null },
            title = { Text("Expense Audit History") },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                    if (auditTrail.isNullOrEmpty()) {
                        Text("No audit events recorded yet.", color = Color.Gray)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(auditTrail) { evt ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(evt.eventType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF60A5FA))
                                            Text(
                                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(evt.timestamp)),
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text("Actor: ${evt.actorId} (${evt.actorRole})", fontSize = 12.sp, color = Color.LightGray)
                                        if (!evt.reason.isNullOrBlank()) {
                                            Text("Reason: ${evt.reason}", fontSize = 12.sp, color = Color(0xFFFCA5A5))
                                        }
                                        if (evt.previousStatus != null || evt.newStatus != null) {
                                            Text("Transition: ${evt.previousStatus ?: "NONE"} → ${evt.newStatus ?: "NONE"}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingAuditExpenseId = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: BusinessExpenseDto,
    category: BusinessExpenseCategoryDto?,
    canApprove: Boolean,
    onSubmit: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onCancel: () -> Unit,
    onViewAudit: () -> Unit
) {
    val statusColor = when (expense.status) {
        "DRAFT" -> Color(0xFF94A3B8)
        "SUBMITTED" -> Color(0xFFF59E0B)
        "APPROVED", "POSTABLE" -> Color(0xFF10B981)
        "REJECTED" -> Color(0xFFEF4444)
        "CANCELLED" -> Color(0xFF6B7280)
        else -> Color.LightGray
    }

    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(expense.expenseDate))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(expense.expenseNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
                    ) {
                        Text(
                            text = expense.status,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "${expense.currency} ${expense.amount}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(expense.description, fontSize = 14.sp, color = Color.White)

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Category: ${category?.name ?: expense.expenseCategoryId}", fontSize = 12.sp, color = Color.LightGray)
                Text("Payment: ${expense.paymentMethod}", fontSize = 12.sp, color = Color.LightGray)
                Text("Date: $formattedDate", fontSize = 12.sp, color = Color.LightGray)
            }

            if (!expense.vendorId.isNullOrBlank() || !expense.jobId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!expense.vendorId.isNullOrBlank()) {
                        Text("Vendor: ${expense.vendorId}", fontSize = 12.sp, color = Color(0xFFA78BFA))
                    }
                    if (!expense.jobId.isNullOrBlank()) {
                        Text("Job: ${expense.jobId}", fontSize = 12.sp, color = Color(0xFF34D399))
                    }
                }
            }

            if (!expense.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("Rejection Reason: ${expense.rejectionReason}", fontSize = 12.sp, color = Color(0xFFFCA5A5))
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFF334155))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("By: ${expense.createdBy}", fontSize = 11.sp, color = Color.Gray)

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onViewAudit) {
                        Text("Audit", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }

                    if (expense.status == "DRAFT" || expense.status == "REJECTED") {
                        TextButton(onClick = onEdit) {
                            Text("Edit", fontSize = 12.sp, color = Color(0xFF60A5FA))
                        }
                        Button(
                            onClick = onSubmit,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Submit", fontSize = 12.sp)
                        }
                    }

                    if (expense.status == "SUBMITTED" && canApprove) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Approve", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onReject,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Reject", fontSize = 12.sp)
                        }
                    }

                    if (expense.status != "CANCELLED" && expense.status != "POSTABLE") {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", fontSize = 12.sp, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseFormDialog(
    existing: BusinessExpenseDto?,
    categories: List<BusinessExpenseCategoryDto>,
    onDismiss: () -> Unit,
    onSave: (CreateBusinessExpenseRequest, Boolean) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(existing?.expenseCategoryId ?: categories.firstOrNull()?.categoryId ?: "") }
    var amount by remember { mutableStateOf(existing?.amount ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var paymentMethod by remember { mutableStateOf(existing?.paymentMethod ?: "CASH") }
    var paymentReference by remember { mutableStateOf(existing?.paymentReference ?: "") }
    var vendorId by remember { mutableStateOf(existing?.vendorId ?: "") }
    var jobId by remember { mutableStateOf(existing?.jobId ?: "") }
    var branchId by remember { mutableStateOf(existing?.branchId ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var autoSubmit by remember { mutableStateOf(false) }

    val isEdit = existing != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Business Expense" else "Record Business Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount (BDT) *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { paymentMethod = it },
                        label = { Text("Payment Method") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = paymentReference,
                    onValueChange = { paymentReference = it },
                    label = { Text("Payment Reference / TrxID") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vendorId,
                        onValueChange = { vendorId = it },
                        label = { Text("Vendor ID (Optional)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = jobId,
                        onValueChange = { jobId = it },
                        label = { Text("Job ID (Optional)") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Internal Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (description.isNotBlank() && amount.isNotBlank()) {
                        onSave(
                            CreateBusinessExpenseRequest(
                                categoryId = selectedCategoryId,
                                amount = amount,
                                description = description,
                                paymentMethod = paymentMethod,
                                paymentReference = paymentReference.takeIf { it.isNotBlank() },
                                vendorId = vendorId.takeIf { it.isNotBlank() },
                                jobId = jobId.takeIf { it.isNotBlank() },
                                branchId = branchId.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() },
                                autoSubmit = autoSubmit
                            ),
                            isEdit
                        )
                    }
                },
                enabled = description.isNotBlank() && amount.isNotBlank()
            ) {
                Text(if (isEdit) "Update" else "Record")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
