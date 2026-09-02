package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.CustomerPaymentAllocationDto
import com.sucharu.sucharupro.data.api.model.CustomerSettlementSummaryDto
import com.sucharu.sucharupro.data.api.model.CustomerUnallocatedPaymentDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.math.BigDecimal

/**
 * Customer Financial Settlement, Payment Allocation & Account Balance Control UI (Module 14 Step 06).
 */
@Composable
fun CustomerSettlementScreen(
    summary: CustomerSettlementSummaryDto?,
    unallocatedPayments: List<CustomerUnallocatedPaymentDto>,
    allocations: List<CustomerPaymentAllocationDto>,
    onAllocateClick: (paymentId: String) -> Unit = {},
    onReverseClick: (allocationId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // 1. Settlement Summary KPIs
        item {
            CustomerSettlementKpiSection(summary = summary)
        }

        // 2. Unallocated Payments (Eligible for Allocation)
        item {
            Text(
                text = "Unallocated Confirmed Payments (${unallocatedPayments.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)
            )
        }

        if (unallocatedPayments.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All confirmed payments are fully allocated.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(unallocatedPayments) { payment ->
                CustomerUnallocatedPaymentCard(
                    payment = payment,
                    onAllocateClick = { onAllocateClick(payment.paymentId) }
                )
            }
        }

        // 3. Payment Allocations History
        item {
            Text(
                text = "Payment Allocation History (${allocations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)
            )
        }

        if (allocations.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.medium),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No payment allocations found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(allocations) { allocation ->
                CustomerAllocationRowCard(
                    allocation = allocation,
                    onReverseClick = { onReverseClick(allocation.allocationId) }
                )
            }
        }
    }
}

@Composable
fun CustomerSettlementKpiSection(
    summary: CustomerSettlementSummaryDto?,
    modifier: Modifier = Modifier
) {
    val invoiced = summary?.totalInvoiced ?: BigDecimal.ZERO
    val paid = summary?.totalPaid ?: BigDecimal.ZERO
    val allocated = summary?.totalAllocated ?: BigDecimal.ZERO
    val unallocated = summary?.totalUnallocated ?: BigDecimal.ZERO
    val outstanding = summary?.totalOutstanding ?: BigDecimal.ZERO

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(text = "Total Invoiced", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳$invoiced", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(text = "Outstanding Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳$outstanding", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(text = "Total Confirmed Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳$paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(text = "Allocated to Invoices", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳$allocated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            OutlinedCard(modifier = Modifier.weight(1f)) {
                Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                    Text(text = "Unallocated Funds", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "৳$unallocated", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun CustomerUnallocatedPaymentCard(
    payment: CustomerUnallocatedPaymentDto,
    onAllocateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Payment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Payment #${payment.paymentNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ৳${payment.totalAmount} | Unallocated: ৳${payment.unallocatedAmount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(onClick = onAllocateClick) {
                Text(text = "Allocate")
            }
        }
    }
}

@Composable
fun CustomerAllocationRowCard(
    allocation: CustomerPaymentAllocationDto,
    onReverseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReversed = allocation.status == "REVERSED"

    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Icon(
                    imageVector = if (isReversed) Icons.Default.Undo else Icons.Default.Receipt,
                    contentDescription = null,
                    tint = if (isReversed) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "Allocated to Invoice #${allocation.invoiceId.takeLast(8)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Amount: ৳${allocation.allocatedAmount} • Status: ${allocation.status}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!isReversed) {
                OutlinedButton(onClick = onReverseClick) {
                    Text(text = "Reverse")
                }
            }
        }
    }
}
