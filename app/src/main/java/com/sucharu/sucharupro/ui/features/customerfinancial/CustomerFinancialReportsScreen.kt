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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.sucharu.sucharupro.data.api.model.CustomerFinancialSummaryReportDto
import com.sucharu.sucharupro.data.api.model.CustomerStatementReportDto
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Customer Financial Reporting, Statement Export & Document Delivery Screen (Module 14 Step 10).
 */
@Composable
fun CustomerFinancialReportsScreen(
    statement: CustomerStatementReportDto?,
    summary: CustomerFinancialSummaryReportDto?,
    onExportCsv: (String) -> Unit = {},
    onExportPdf: (String) -> Unit = {},
    onViewStatementClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        item {
            Text(
                text = "Financial Reports & Statement Export",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Export authoritative financial reports, chronological statements, and receivable intelligence documents.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Customer Account Statement Card
        item {
            ReportActionCard(
                title = "Customer Account Statement",
                description = "Full chronological ledger timeline with opening balance, debits, credits, and running balance.",
                icon = Icons.Default.Receipt,
                onCsvClick = { onExportCsv("CUSTOMER_STATEMENT") },
                onPdfClick = { onExportPdf("CUSTOMER_STATEMENT") }
            )
        }

        // 2. Invoices Report Card
        item {
            ReportActionCard(
                title = "Customer Invoices Report",
                description = "Complete listing of invoices, totals, paid amounts, due balances, and overdue days.",
                icon = Icons.Default.TableChart,
                onCsvClick = { onExportCsv("INVOICE_REPORT") },
                onPdfClick = { onExportPdf("INVOICE_REPORT") }
            )
        }

        // 3. Payment History Report Card
        item {
            ReportActionCard(
                title = "Payment History Report",
                description = "Historical log of recorded payments, allocation breakdowns, and payment methods.",
                icon = Icons.Default.Payment,
                onCsvClick = { onExportCsv("PAYMENT_HISTORY") },
                onPdfClick = { onExportPdf("PAYMENT_HISTORY") }
            )
        }

        // 4. Receivable Aging Report Card
        item {
            ReportActionCard(
                title = "Receivable Aging Analysis",
                description = "Aging breakdown into 6 canonical tiers: Current, 1-7d, 8-30d, 31-60d, 61-90d, 90d+.",
                icon = Icons.Default.TrendingUp,
                onCsvClick = { onExportCsv("RECEIVABLE_AGING") },
                onPdfClick = { onExportPdf("RECEIVABLE_AGING") }
            )
        }

        // 5. Settlement & Allocation Report Card
        item {
            ReportActionCard(
                title = "Settlement & Allocation Report",
                description = "Authoritative settlement report with invoiced, paid, allocated, unallocated, and available credit.",
                icon = Icons.Default.CheckCircle,
                onCsvClick = { onExportCsv("SETTLEMENT_REPORT") },
                onPdfClick = { onExportPdf("SETTLEMENT_REPORT") }
            )
        }

        // 6. Credit Risk Report Card
        item {
            ReportActionCard(
                title = "Credit Limit & Risk Report",
                description = "Approved credit limits, payment terms, net exposure, advance requirements, and hold status.",
                icon = Icons.Default.AccountBalance,
                onCsvClick = { onExportCsv("CREDIT_RISK_REPORT") },
                onPdfClick = { onExportPdf("CREDIT_RISK_REPORT") }
            )
        }

        // 7. Collection Activity Report Card
        item {
            ReportActionCard(
                title = "Collection Follow-up Report",
                description = "Summary of scheduled collection actions, payment promises, outcomes, and overdue tallies.",
                icon = Icons.Default.Assignment,
                onCsvClick = { onExportCsv("COLLECTION_REPORT") },
                onPdfClick = { onExportPdf("COLLECTION_REPORT") }
            )
        }

        // 8. Reconciliation Report Card
        item {
            ReportActionCard(
                title = "Ledger Reconciliation Report",
                description = "Diagnostic verification comparing invoice receivables with ledger entries and available credits.",
                icon = Icons.Default.CheckCircle,
                onCsvClick = { onExportCsv("RECONCILIATION_REPORT") },
                onPdfClick = { onExportPdf("RECONCILIATION_REPORT") }
            )
        }
    }
}

@Composable
private fun ReportActionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onCsvClick: () -> Unit,
    onPdfClick: () -> Unit
) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = onCsvClick) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "CSV",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "CSV Export")
                }
                Spacer(modifier = Modifier.size(8.dp))
                Button(onClick = onPdfClick) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "PDF / Document")
                }
            }
        }
    }
}
