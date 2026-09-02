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
import com.sucharu.sucharupro.data.api.model.VendorPortalUnifiedWorkspaceSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalUnifiedWorkspaceScreen(
    summary: VendorPortalUnifiedWorkspaceSummaryDto,
    onNavigateSection: (String) -> Unit = {},
    onGlobalSearchClick: () -> Unit = {},
    onNotificationCenterClick: () -> Unit = {},
    onAnalyticsHubClick: () -> Unit = {},
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
                        text = "Vendor Self-Service Portal",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    TextButton(onClick = onGlobalSearchClick) {
                        Text(text = "Search", color = Color(0xFF38BDF8))
                    }
                    TextButton(onClick = onNotificationCenterClick) {
                        Text(
                            text = if (summary.unreadNotificationCount > 0) "Alerts (${summary.unreadNotificationCount})" else "Alerts",
                            color = if (summary.unreadNotificationCount > 0) Color(0xFFEF4444) else Color(0xFF94A3B8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Partner Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = summary.vendorName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Vendor ID: ${summary.vendorId}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Badge(containerColor = Color(0xFF10B981)) {
                                Text(
                                    text = summary.complianceStatus,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Active POs", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(text = "${summary.activePoCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text(text = "Pending Invoices", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(text = "${summary.pendingInvoiceCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text(text = "Disputes", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(text = "${summary.openDisputeCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                            }
                            Column {
                                Text(text = "Score", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                Text(text = "${summary.overallPerformanceScore}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            }
                        }
                    }
                }
            }

            // Quick Hub Banner
            item {
                Button(
                    onClick = onAnalyticsHubClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text(text = "Open Unified Analytics Hub & Trends", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Workspace Navigation Sections
            item {
                Text(
                    text = "Operational Workspace Sections",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            items(summary.navigationSections.filter { it.isVisible }.sortedBy { it.order }) { sec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateSection(sec.route) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sec.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        if (sec.badgeCount > 0) {
                            Badge(containerColor = Color(0xFF3B82F6)) {
                                Text(
                                    text = "${sec.badgeCount}",
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
