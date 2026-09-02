package com.sucharu.sucharupro.ui.features.finance.expense

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.finance.ExpenseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailsScreen(
    viewModel: ExpenseDetailsViewModel,
    callerRole: UserRole,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.expense?.let { "Expense #${it.expenseNo}" } ?: "Expense Details") },
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
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.errorMessage != null && state.expense == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
            state.expense != null -> {
                val expense = state.expense!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = state.category?.categoryName ?: "Uncategorized",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    ExpenseStatusBadge(status = expense.status)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Amount", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${expense.amount.formatted()} ${expense.currency}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                    ExpensePaymentMethodBadge(method = expense.paymentMethod)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(text = "Description: ${expense.description}", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (!expense.paymentReference.isNullOrBlank()) {
                                    Text(text = "Payment Ref: ${expense.paymentReference}", fontSize = 13.sp)
                                }
                                if (!expense.vendorId.isNullOrBlank()) {
                                    Text(text = "Vendor Link: ${expense.vendorId}", fontSize = 13.sp)
                                }
                                 val refType = expense.referenceType
                                if (refType != null && !expense.referenceId.isNullOrBlank()) {
                                    Text(text = "Business Ref: ${refType.name} #${expense.referenceId}", fontSize = 12.sp)
                                }
                                Text(text = "Expense Date: ${dateFormat.format(Date(expense.expenseDate))}", fontSize = 13.sp)

                                if (!expense.financialTransactionId.isNullOrBlank()) {
                                    Text(
                                        text = "Ledger Transaction ID: ${expense.financialTransactionId}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (!expense.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Notes: ${expense.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Action Controls
                    item {
                        if (callerRole.isInternal && !expense.status.isTerminal) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (expense.status == ExpenseStatus.DRAFT) {
                                        Button(
                                            onClick = viewModel::submitExpense,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Submit")
                                        }
                                    }

                                    if (expense.status == ExpenseStatus.PENDING) {
                                        Button(
                                            onClick = viewModel::approveExpense,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Approve")
                                        }
                                    }

                                    if (expense.status == ExpenseStatus.APPROVED || expense.status == ExpenseStatus.PENDING) {
                                        Button(
                                            onClick = { viewModel.postExpense() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Post to Ledger")
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (expense.status == ExpenseStatus.PENDING) {
                                        OutlinedButton(
                                            onClick = { viewModel.rejectExpense("Rejected by management") },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.cancelExpense("Cancelled by user") },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }

                    // Activity Audit Trail
                    item {
                        Text(
                            text = "Activity Audit Trail",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.activityEvents, key = { it.eventId }) { event ->
                        ExpenseActivityTimelineItem(event = event)
                    }

                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }
    }
}
