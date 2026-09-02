package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.CustomerLedgerEntryDto
import com.sucharu.sucharupro.data.api.model.CustomerReceivableReconciliationDto
import com.sucharu.sucharupro.data.api.model.CustomerStatementDto
import com.sucharu.sucharupro.data.api.model.CustomerStatementSummaryDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing
import java.math.BigDecimal

/**
 * Customer Ledger, Financial Statement & Reconciliation UI Component (Module 14 Step 05).
 */
@Composable
fun CustomerLedgerStatementScreen(
    statement: CustomerStatementDto?,
    summary: CustomerStatementSummaryDto?,
    reconciliation: CustomerReceivableReconciliationDto?,
    onReconcileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // 1. Header Overview
        item {
            CustomerFinancialHeader(summary = summary, statement = statement)
        }

        // 2. Reconciliation Diagnostic Status
        if (reconciliation != null) {
            item {
                CustomerReconciliationStatusCard(
                    reconciliation = reconciliation,
                    onReconcileClick = onReconcileClick
                )
            }
        }

        // 3. Statement Summary KPI Cards
        item {
            CustomerFinancialKpiGrid(summary = summary)
        }

        // 4. Ledger Transaction History
        item {
            Text(
                text = "Chronological Statement Ledger",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = MaterialTheme.spacing.small)
            )
        }

        if (statement == null || statement.entries.isEmpty()) {
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No financial transactions found for this period.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(statement.entries) { entry ->
                CustomerLedgerEntryRow(entry = entry)
            }
        }
    }
}

@Composable
fun CustomerFinancialHeader(
    summary: CustomerStatementSummaryDto?,
    statement: CustomerStatementDto?,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = statement?.customerDisplayName ?: "Customer Financial Statement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Account: ${statement?.accountNumber ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerReconciliationStatusCard(
    reconciliation: CustomerReceivableReconciliationDto,
    onReconcileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConsistent = reconciliation.isConsistent
    val bgCardColor = if (isConsistent) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    val iconTint = if (isConsistent) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                Icon(
                    imageVector = if (isConsistent) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = iconTint
                )
                Text(
                    text = if (isConsistent) "Receivable State: Consistent" else "Receivable Discrepancy Detected (${reconciliation.discrepancyCount})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isConsistent) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Invoice Due: ৳${reconciliation.invoiceTotalReceivable}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Ledger Balance: ৳${reconciliation.ledgerCalculatedBalance}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Credit Balance: ৳${reconciliation.availableCreditBalance}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CustomerFinancialKpiGrid(
    summary: CustomerStatementSummaryDto?,
    modifier: Modifier = Modifier
) {
    val receivable = summary?.currentReceivableBalance ?: BigDecimal.ZERO
    val credit = summary?.availableCreditBalance ?: BigDecimal.ZERO
    val net = summary?.netBalance ?: BigDecimal.ZERO

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        OutlinedCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                Text(text = "Receivable Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "৳$receivable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
            }
        }

        OutlinedCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                Text(text = "Available Credit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "৳$credit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
            }
        }

        OutlinedCard(
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
                Text(text = "Net Position", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "৳$net",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun CustomerLedgerEntryRow(
    entry: CustomerLedgerEntryDto,
    modifier: Modifier = Modifier
) {
    val isDebit = entry.debitAmount > BigDecimal.ZERO
    val isCredit = entry.creditAmount > BigDecimal.ZERO

    AppCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.small)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                ) {
                    Icon(
                        imageVector = if (isDebit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isDebit) Color(0xFFC62828) else Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ref #${entry.referenceNumber ?: entry.referenceId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isDebit) {
                        Text(
                            text = "+৳${entry.debitAmount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    } else if (isCredit) {
                        Text(
                            text = "-৳${entry.creditAmount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                    Text(
                        text = "Bal: ৳${entry.balanceAfter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
