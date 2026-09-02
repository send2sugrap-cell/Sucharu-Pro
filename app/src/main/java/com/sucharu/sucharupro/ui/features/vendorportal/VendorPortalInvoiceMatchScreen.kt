package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
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
import com.sucharu.sucharupro.data.api.model.VendorPortalInvoiceMatchLineSummaryDto
import com.sucharu.sucharupro.data.api.model.VendorPortalInvoiceMatchSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalInvoiceMatchScreen(
    matchSummary: VendorPortalInvoiceMatchSummaryDto,
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
                        text = "3-Way Match Breakdown",
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
            // Overall Status Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (matchSummary.matchStatus) {
                        "MATCHED" -> Color(0xFF14532D)
                        "EXCEPTION", "MISMATCH" -> Color(0xFF7F1D1D)
                        else -> Color(0xFF78350F)
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Overall Status: ${matchSummary.matchStatus.replace("_", " ")}",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Exceptions: ${matchSummary.exceptionCount} • Total Variance: ${String.format("%.2f", matchSummary.totalVariance)}",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                }
            }

            Text("Line-by-Line Comparison", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(matchSummary.lines) { line ->
                    MatchLineCard(line = line)
                }
            }
        }
    }
}

@Composable
fun MatchLineCard(line: VendorPortalInvoiceMatchLineSummaryDto) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(line.description, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(
                    line.matchStatus.replace("_", " "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (line.matchStatus == "MATCHED") Color(0xFF4ADE80) else Color(0xFFF87171)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PO Qty: ${line.orderedQuantity}", fontSize = 12.sp, color = Color.Gray)
                Text("Received: ${line.receivedQuantity}", fontSize = 12.sp, color = Color.Gray)
                Text("Invoiced: ${line.invoicedQuantity}", fontSize = 12.sp, color = Color(0xFF38BDF8))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PO Price: ${line.orderedUnitPrice}", fontSize = 12.sp, color = Color.Gray)
                Text("Invoiced Price: ${line.invoicedUnitPrice}", fontSize = 12.sp, color = Color(0xFF38BDF8))
                Text("Qty Var: ${line.quantityVariance}", fontSize = 12.sp, color = if (line.quantityVariance != 0.0) Color(0xFFF87171) else Color.Gray)
            }

            if (!line.exceptionReason.isNullOrBlank()) {
                Text("Reason: ${line.exceptionReason}", fontSize = 12.sp, color = Color(0xFFFCA5A5))
            }
        }
    }
}
