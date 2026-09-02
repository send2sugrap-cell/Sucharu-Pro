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
import com.sucharu.sucharupro.data.api.model.VendorPortalWorkOrderSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalWorkOrderListScreen(
    workOrders: List<VendorPortalWorkOrderSummaryDto>,
    selectedStatus: String? = null,
    onStatusFilterChange: (String?) -> Unit = {},
    onWorkOrderClick: (String) -> Unit = {},
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
                        text = "Vendor Work Orders",
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
                    listOf(null, "ASSIGNED", "READY", "RELEASED", "IN_PROGRESS", "COMPLETED").forEach { status ->
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

                if (workOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No Work Orders found", color = Color(0xFF64748B), fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(workOrders) { wo ->
                            VendorWorkOrderCard(wo = wo, onClick = { onWorkOrderClick(wo.workOrderId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorWorkOrderCard(wo: VendorPortalWorkOrderSummaryDto, onClick: () -> Unit) {
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
                    text = wo.workOrderNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
                WorkOrderStatusBadge(status = wo.status)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = wo.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: ${wo.quantity} ${wo.unitOfMeasure} • ${wo.capabilityType}",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Est: ${wo.currency} ${wo.estimatedAmount}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1)
                )
                wo.latestProgressPercentage?.let { prog ->
                    Text(
                        text = "Progress: ${prog.toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
            if (wo.openBlockersCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "⚠ ${wo.openBlockersCount} Open Blocker(s)",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun WorkOrderStatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "ASSIGNED", "DRAFT" -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        "READY", "RELEASED" -> Color(0xFF065F46) to Color(0xFF34D399)
        "IN_PROGRESS" -> Color(0xFF78350F) to Color(0xFFFBBF24)
        "COMPLETED" -> Color(0xFF064E3B) to Color(0xFF10B981)
        "ON_HOLD" -> Color(0xFF4C1D95) to Color(0xFFA78BFA)
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
