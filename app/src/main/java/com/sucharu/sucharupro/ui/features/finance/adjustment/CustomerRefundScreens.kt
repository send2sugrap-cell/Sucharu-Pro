package com.sucharu.sucharupro.ui.features.finance.adjustment

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerRefund
import com.sucharu.sucharupro.domain.model.finance.CustomerRefundMethod
import com.sucharu.sucharupro.domain.model.finance.FinancialAdjustmentStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.CustomerRefundRepository
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRefundListScreen(
    refundRepository: CustomerRefundRepository,
    projectId: String,
    callerRole: UserRole,
    onRefundClick: (String) -> Unit,
    onCreateRefundClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val refundsState = refundRepository.observeRefunds(projectId, callerRole).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customer Refunds") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (callerRole.isInternal) {
                FloatingActionButton(
                    onClick = onCreateRefundClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Refund")
                }
            }
        },
        modifier = modifier
    ) { padding ->
        val refunds = refundsState.value
        when {
            refunds == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            refunds.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = "No customer refunds recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(refunds, key = { it.refundId }) { refund ->
                        CustomerRefundCard(
                            refund = refund,
                            onClick = { onRefundClick(refund.refundId) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRefundDetailsScreen(
    refundRepository: CustomerRefundRepository,
    refundId: String,
    callerRole: UserRole,
    currentActorId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var refund by remember { mutableStateOf<CustomerRefund?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    fun reload() {
        scope.launch {
            isLoading = true
            val res = refundRepository.getRefundById(refundId, callerRole)
            if (res is DomainResult.Success) {
                refund = res.data
            } else if (res is DomainResult.Error) {
                errorMessage = res.message
            }
            isLoading = false
        }
    }

    remember(refundId) { reload(); true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(refund?.let { "Refund #${it.refundNo}" } ?: "Refund Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            refund != null -> {
                val ref = refund!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Customer #${ref.customerId}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                FinancialAdjustmentStatusBadge(status = ref.status)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "${ref.amount.formatted()} ${ref.currency}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Disbursement: ${ref.refundMethod.defaultLabel}", fontSize = 13.sp)
                            ref.refundReference?.let { Text(text = "Reference: $it", fontSize = 13.sp) }
                            Text(text = "Reason: ${ref.reason}", fontSize = 13.sp)
                            ref.financialTransactionId?.let {
                                Text(text = "Ledger Txn: $it", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    if (callerRole.isInternal && !ref.status.isTerminal) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (ref.status == FinancialAdjustmentStatus.DRAFT) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            refundRepository.submitRefund(refundId, currentActorId, callerRole)
                                            reload()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Submit")
                                }
                            }

                            if (ref.status == FinancialAdjustmentStatus.PENDING) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            refundRepository.approveRefund(refundId, currentActorId, callerRole)
                                            reload()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Approve")
                                }
                            }

                            if (ref.status == FinancialAdjustmentStatus.APPROVED || ref.status == FinancialAdjustmentStatus.PENDING) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            refundRepository.postRefund(refundId, null, currentActorId, callerRole)
                                            reload()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Post Refund")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerRefundFormScreen(
    refundRepository: CustomerRefundRepository,
    projectId: String,
    callerRole: UserRole,
    currentActorId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var customerId by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(CustomerRefundMethod.CASH) }
    var reference by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Customer Refund") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }

            OutlinedTextField(
                value = customerId,
                onValueChange = { customerId = it },
                label = { Text("Customer ID *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Refund Amount (BDT) *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Disbursement Channel *", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomerRefundMethod.values().forEach { m ->
                    FilterChip(
                        selected = method == m,
                        onClick = { method = m },
                        label = { Text(m.defaultLabel) }
                    )
                }
            }

            if (method.requiresReference) {
                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Payment / Trx Reference *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Refund Reason *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val amount = amountText.toBigDecimalOrNull()
                    if (amount == null || amount <= BigDecimal.ZERO) {
                        errorMessage = "Please enter a valid positive refund amount."
                        return@Button
                    }
                    scope.launch {
                        isSubmitting = true
                        errorMessage = null
                        val res = refundRepository.createRefund(
                            projectId = projectId,
                            customerId = customerId.trim(),
                            amount = Money(amount),
                            refundMethod = method,
                            refundReference = reference.trim().ifEmpty { null },
                            reason = reason.trim(),
                            actorId = currentActorId,
                            callerRole = callerRole
                        )
                        isSubmitting = false
                        if (res is DomainResult.Success) {
                            onNavigateBack()
                        } else if (res is DomainResult.Error) {
                            errorMessage = res.message
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Refund")
                }
            }
        }
    }
}
