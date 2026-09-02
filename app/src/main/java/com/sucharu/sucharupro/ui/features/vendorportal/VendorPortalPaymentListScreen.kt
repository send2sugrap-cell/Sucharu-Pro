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
import com.sucharu.sucharupro.data.api.model.VendorPortalPaymentSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalPaymentListScreen(
    payments: List<VendorPortalPaymentSummaryDto>,
    onPaymentClick: (String) -> Unit = {},
    onViewInvoicesClick: () -> Unit = {},
    onViewKpiClick: () -> Unit = {},
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
                        text = "Vendor Payments & Remittance",
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
        ) {
            if (payments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No settlement or payment records found.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(payments) { p ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPaymentClick(p.settlementId) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = p.settlementNumber,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Surface(
                                        color = if (p.paymentStatus == "PAID") Color(0xFF14532D) else Color(0xFF1E3A8A),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = p.paymentStatus,
                                            color = if (p.paymentStatus == "PAID") Color(0xFF4ADE80) else Color(0xFF93C5FD),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Amount: ${p.currency} ${String.format("%.2f", p.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                                    Text("Method: ${p.paymentMethod}", fontSize = 12.sp, color = Color.LightGray)
                                }

                                if (p.referenceNumber != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Bank Ref: ${p.referenceNumber}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
