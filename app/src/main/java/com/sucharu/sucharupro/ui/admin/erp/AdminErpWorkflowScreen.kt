package com.sucharu.sucharupro.ui.admin.erp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Form 05 — Admin ERP / Order / Fulfillment Integration Management Workspace.
 */
@Composable
fun AdminErpWorkflowScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminErpWorkflowViewModel = viewModel { AdminErpWorkflowViewModel() }
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Page Header
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FORM 05 • ERP / Order / Fulfillment Integration",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "ইআরপি, অর্ডার ও ফুলফিলমেন্ট ইন্টিগ্রেশন হাব",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "কাস্টমার অ্যাকশন থেকে ক্যানোনিকাল ইআরপি ওয়ার্কফ্লো (অর্ডার ➔ প্রোডাকশন ➔ কিউসি ➔ চালান ➔ ইনভয়েস) হ্যান্ডঅফ কন্ট্রোল",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 01: Canonical ERP Pipeline Overview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "১. ক্যানোনিকাল ইআরপি ওয়ার্কফ্লো ফ্লো (Canonical ERP Pipeline)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Customer Action ➔ Form 01 Content ➔ Form 02 Design ➔ Form 03 Offer ➔ Form 04 Pricing ➔ Form 05 ERP Orchestration ➔ Module 03 Order ➔ Module 04 Production ➔ Module 06 QC ➔ Module 07 Inventory ➔ Module 08 Delivery ➔ Module 09 Finance",
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 02: E2E Pipeline Simulator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0284C7).copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, Color(0xFF0284C7))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚡ কাস্টমার অর্ডার ইভালুয়েশন ও ইআরপি পাইপলাইন সিমুলেটর:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.testE2eOrderOrchestration() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ইআরপি অর্ডার অর্কেস্ট্রেশন টেস্ট রান করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                viewModel.lastResult?.let { res ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "অর্ডার অর্কেস্ট্রেশন রেজাল্ট:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            Text(text = "Orchestration ID: ${res.orchestrationId} | Status: ${res.workflowStatus}", fontSize = 11.sp, color = Color.White)
                            Text(text = "Order ID: ${res.orderId} | Price Snapshot: ৳${res.grandTotalAmount.toInt()}", fontSize = 11.sp, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECTION 03: Recent Orchestration Records
        Text(text = "সাম্প্রতিক ইআরপি অর্কেস্ট্রেশন রেকর্ডস (${viewModel.orchestrationsList.size})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
        Spacer(modifier = Modifier.height(8.dp))

        viewModel.orchestrationsList.forEach { orch ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(4.dp)) {
                            Text(text = orch.customerAction.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        Surface(color = Color(0xFF10B981), shape = RoundedCornerShape(4.dp)) {
                            Text(text = orch.workflowStatus.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Order #${orch.orderId ?: "N/A"} • Amount: ৳${orch.orderAmount.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Customer: ${orch.customerId} • Product: ${orch.productId}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
            }
        }

        viewModel.statusMessage?.let { msg ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = msg, color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
