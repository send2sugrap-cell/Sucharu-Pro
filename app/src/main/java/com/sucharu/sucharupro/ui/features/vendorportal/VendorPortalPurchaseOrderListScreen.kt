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
import com.sucharu.sucharupro.data.api.model.VendorPortalPurchaseOrderSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalPurchaseOrderListScreen(
    orders: List<VendorPortalPurchaseOrderSummaryDto>,
    selectedStatus: String? = null,
    onStatusFilterChange: (String?) -> Unit = {},
    onOrderClick: (String) -> Unit = {},
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
                        text = "Vendor Purchase Orders",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Status Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null, "ISSUED", "ACKNOWLEDGED", "IN_PROGRESS", "RECEIVED", "COMPLETED").forEach { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusFilterChange(status) },
                            label = { Text(status ?: "ALL", color = if (isSelected) Color.White else Color(0xFF94A3B8)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF3B82F6),
                                containerColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }

                if (orders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No Purchase Orders found", color = Color(0xFF64748B), fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders) { po ->
                            VendorPoCard(po = po, onClick = { onOrderClick(po.purchaseOrderId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorPoCard(po: VendorPortalPurchaseOrderSummaryDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = po.orderNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
                PoStatusBadge(status = po.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total: ${po.currency} ${po.totalAmount}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                po.acknowledgementStatus?.let { ack ->
                    Text(
                        text = "Ack: $ack",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (ack == "ACKNOWLEDGED") Color(0xFF34D399) else Color(0xFFFBBF24)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Location: ${po.deliveryLocation ?: "Standard"}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                if (po.openBlockersCount > 0) {
                    Text(
                        text = "⚠ ${po.openBlockersCount} Open Blocker(s)",
                        fontSize = 12.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PoStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "ISSUED" -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        "ACKNOWLEDGED" -> Color(0xFF065F46) to Color(0xFF34D399)
        "IN_PROGRESS" -> Color(0xFF78350F) to Color(0xFFFBBF24)
        "RECEIVED", "COMPLETED" -> Color(0xFF064E3B) to Color(0xFF10B981)
        "CANCELLED" -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        else -> Color(0xFF1E293B) to Color(0xFF94A3B8)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
