package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun VendorPortalInvoiceListScreen(
    invoices: List<VendorPortalInvoiceSummaryDto>,
    selectedStatus: String? = null,
    onStatusFilterChange: (String?) -> Unit = {},
    onCreateInvoiceClick: () -> Unit = {},
    onInvoiceClick: (String) -> Unit = {},
    onViewPaymentsClick: () -> Unit = {},
    onViewFinancialSummaryClick: () -> Unit = {},
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
                        text = "Vendor Invoices & Billing",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = onCreateInvoiceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("+ Submit Invoice", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E293B)) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    label = { Text("Invoices", color = Color.White) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewPaymentsClick,
                    label = { Text("Payments", color = Color.Gray) },
                    icon = {}
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onViewFinancialSummaryClick,
                    label = { Text("KPI Summary", color = Color.Gray) },
                    icon = {}
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Status filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val statuses = listOf(null, "DRAFT", "SUBMITTED", "MATCHED", "APPROVED", "REJECTED")
                statuses.forEach { st ->
                    FilterChip(
                        selected = selectedStatus == st,
                        onClick = { onStatusFilterChange(st) },
                        label = { Text(st ?: "All", fontSize = 12.sp) }
                    )
                }
            }

            if (invoices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No invoices found.", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(invoices) { inv ->
                        VendorInvoiceCard(invoice = inv, onClick = { onInvoiceClick(inv.invoiceId) })
                    }
                }
            }
        }
    }
}

@Composable
fun VendorInvoiceCard(
    invoice: VendorPortalInvoiceSummaryDto,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                InvoiceStatusBadge(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Vendor Ref: ${invoice.vendorInvoiceNumber} • PO: ${invoice.orderNumber}",
                fontSize = 13.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Amount", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${invoice.currency} ${String.format("%.2f", invoice.totalAmount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Match Status", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        invoice.matchStatus.replace("_", " "),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (invoice.matchStatus) {
                            "MATCHED" -> Color(0xFF22C55E)
                            "EXCEPTION", "MISMATCH" -> Color(0xFFEF4444)
                            else -> Color(0xFFFBBF24)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(status: String) {
    val (bg, fg) = when (status) {
        "APPROVED", "POSTED" -> Color(0xFF14532D) to Color(0xFF4ADE80)
        "MATCHED" -> Color(0xFF1E3A8A) to Color(0xFF93C5FD)
        "REJECTED", "CANCELLED" -> Color(0xFF7F1D1D) to Color(0xFFFCA5A5)
        "SUBMITTED", "UNDER_REVIEW" -> Color(0xFF78350F) to Color(0xFFFDE68A)
        else -> Color(0xFF334155) to Color(0xFFCBD5E1)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status.replace("_", " "),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
