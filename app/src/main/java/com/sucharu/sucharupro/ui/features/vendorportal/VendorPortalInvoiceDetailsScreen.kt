package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalInvoiceSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalInvoiceDetailsScreen(
    invoice: VendorPortalInvoiceSummaryDto,
    onViewMatchClick: (String) -> Unit = {},
    onRespondClick: (String) -> Unit = {},
    onViewEvidenceClick: (String) -> Unit = {},
    onViewActivityClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Invoice: ${invoice.invoiceNumber}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status and Match Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InvoiceStatusBadge(status = invoice.status)
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "3-Way Match: ${invoice.matchStatus}",
                        fontSize = 12.sp,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Financial Summary Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Financial Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    HorizontalDivider(color = Color.DarkGray)

                    FinancialMetricRow("Total Invoiced", "${invoice.currency} ${String.format("%.2f", invoice.totalAmount)}")
                    FinancialMetricRow("Approved Amount", "${invoice.currency} ${String.format("%.2f", invoice.approvedAmount)}")
                    FinancialMetricRow("Paid Amount", "${invoice.currency} ${String.format("%.2f", invoice.paidAmount)}")
                    FinancialMetricRow("Outstanding Amount", "${invoice.currency} ${String.format("%.2f", invoice.outstandingAmount)}")
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onViewMatchClick(invoice.invoiceId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("3-Way Match", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { onRespondClick(invoice.invoiceId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Respond / Clarify", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onViewEvidenceClick(invoice.invoiceId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Evidence", fontSize = 12.sp, color = Color.LightGray)
                }

                OutlinedButton(
                    onClick = { onViewActivityClick(invoice.invoiceId) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Timeline", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun FinancialMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
