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
import com.sucharu.sucharupro.data.api.model.VendorPortalDisputeSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalDisputeListScreen(
    disputes: List<VendorPortalDisputeSummaryDto>,
    selectedStatus: String? = null,
    onStatusFilterChange: (String?) -> Unit = {},
    onRaiseDisputeClick: () -> Unit = {},
    onDisputeClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    val statuses = listOf("ALL", "OPEN", "VENDOR_RESPONSE_REQUIRED", "VENDOR_RESPONDED", "UNDER_REVIEW", "RESOLUTION_PROPOSED", "RESOLVED", "CLOSED")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vendor Disputes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = onRaiseDisputeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Raise Dispute", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.take(5).forEach { status ->
                    val isSelected = (status == "ALL" && selectedStatus == null) || (status == selectedStatus)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (status == "ALL") onStatusFilterChange(null) else onStatusFilterChange(status)
                        },
                        label = { Text(status, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        )
                    )
                }
            }

            if (disputes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No disputes found.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(disputes) { disp ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDisputeClick(disp.disputeId) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = disp.disputeReference,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = disp.status,
                                        color = when (disp.status) {
                                            "RESOLVED", "CLOSED" -> Color(0xFF10B981)
                                            "VENDOR_RESPONSE_REQUIRED", "RESOLUTION_PROPOSED" -> Color(0xFFF59E0B)
                                            else -> Color(0xFFEF4444)
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = disp.subject, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Type: ${disp.disputeType} • Priority: ${disp.priority} • Req: ${disp.requestedResolution}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
