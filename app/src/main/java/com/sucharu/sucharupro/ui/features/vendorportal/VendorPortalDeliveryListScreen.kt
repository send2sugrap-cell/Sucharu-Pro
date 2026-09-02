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
import com.sucharu.sucharupro.data.api.model.VendorPortalDeliveryNoticeDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDeliveryListScreen(
    deliveryNotices: List<VendorPortalDeliveryNoticeDto>,
    selectedStatus: String? = null,
    onStatusFilterChange: (String?) -> Unit = {},
    onCreateNoticeClick: () -> Unit = {},
    onNoticeClick: (String) -> Unit = {},
    onViewReceivingClick: () -> Unit = {},
    onViewQualityClick: () -> Unit = {},
    onViewExceptionsClick: () -> Unit = {},
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
                        text = "Delivery & Quality Workspace",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = onCreateNoticeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("+ New ASN", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
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
                // Quick Navigation Hub
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewReceivingClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Receiving", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onViewQualityClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFA855F7)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quality", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onViewExceptionsClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF97316)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exceptions", fontSize = 12.sp)
                    }
                }

                // Filter chips
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null, "DRAFT", "SUBMITTED", "IN_TRANSIT", "DELIVERED").forEach { status ->
                        val isSelected = selectedStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { onStatusFilterChange(status) },
                            label = { Text(status ?: "ALL", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                // Delivery notices list
                if (deliveryNotices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No delivery notices found.", color = Color(0xFF64748B), fontSize = 15.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(deliveryNotices) { notice ->
                            DeliveryNoticeCard(notice = notice, onClick = { onNoticeClick(notice.noticeId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryNoticeCard(
    notice: VendorPortalDeliveryNoticeDto,
    onClick: () -> Unit
) {
    val statusColor = when (notice.status) {
        "DRAFT" -> Color(0xFFE2E8F0)
        "SUBMITTED" -> Color(0xFF38BDF8)
        "ACKNOWLEDGED" -> Color(0xFF818CF8)
        "IN_TRANSIT" -> Color(0xFFFBBF24)
        "DELIVERED" -> Color(0xFF34D399)
        "CANCELLED" -> Color(0xFFF87171)
        else -> Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    text = notice.noticeNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = notice.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Purchase Order: ${notice.orderNumber}",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Items: ${notice.items.size}",
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0)
                )
                if (notice.carrierName != null) {
                    Text(
                        text = "Carrier: ${notice.carrierName}",
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }
    }
}
