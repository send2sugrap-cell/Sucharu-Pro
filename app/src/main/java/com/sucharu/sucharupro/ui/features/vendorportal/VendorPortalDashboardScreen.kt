package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.data.api.model.*

/**
 * Production Jetpack Compose Vendor Portal Dashboard Screen (Module 13 Step 02).
 */
@Composable
fun VendorPortalDashboardScreen(
    dashboard: VendorPortalDashboardDto?,
    isLoading: Boolean,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A) // Dark navy / near-black foundation
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMessage, color = Color(0xFFF87171), style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))) {
                        Text("Retry", color = Color.Black)
                    }
                }
            }
        } else if (dashboard != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Header Banner
                item {
                    VendorDashboardHeader(dashboard)
                }

                // 2. KPI Cards Grid / Row
                item {
                    Text(
                        "Key Performance Indicators",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(dashboard.kpis) { kpi ->
                            VendorKpiCard(kpi)
                        }
                    }
                }

                // 3. Operational & Financial Overview
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        dashboard.operations?.let { ops ->
                            VendorOperationsCard(ops, modifier = Modifier.weight(1f))
                        }
                        dashboard.financials?.let { fin ->
                            VendorFinancialsCard(fin, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 4. Quality & Compliance Overview
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        dashboard.quality?.let { q ->
                            VendorQualityCard(q, modifier = Modifier.weight(1f))
                        }
                        dashboard.compliance?.let { comp ->
                            VendorComplianceCard(comp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // 5. Recent Activity
                item {
                    Text(
                        "Recent Activity",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (dashboard.recentActivities.isEmpty()) {
                                Text("No recent activity recorded.", color = Color(0xFF94A3B8))
                            } else {
                                dashboard.recentActivities.forEach { act ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(act.title, color = Color.White, fontWeight = FontWeight.Medium)
                                            Text(act.description, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text(act.eventType, color = Color(0xFF38BDF8), style = MaterialTheme.typography.labelSmall)
                                    }
                                    HorizontalDivider(color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendorDashboardHeader(dashboard: VendorPortalDashboardDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0284C7).copy(alpha = 0.2f), Color(0xFF0F172A))
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        dashboard.vendorName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF0284C7).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            dashboard.vendorCode,
                            color = Color(0xFF38BDF8),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Role: ${dashboard.portalRole} • Account Status: ${dashboard.accountStatus}",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun VendorKpiCard(kpi: VendorPortalKpiDto) {
    val statusColor = when (kpi.status) {
        "GOOD" -> Color(0xFF4ADE80)
        "WARNING" -> Color(0xFFFBBF24)
        "CRITICAL" -> Color(0xFFF87171)
        else -> Color(0xFF38BDF8)
    }

    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(kpi.label, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                kpi.value,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    kpi.category,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun VendorOperationsCard(ops: VendorPortalOperationalSummaryDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Operations Summary", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Active POs", color = Color(0xFF94A3B8))
                Text("${ops.activePurchaseOrders}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Open Work Orders", color = Color(0xFF94A3B8))
                Text("${ops.openWorkOrders}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("On-Time Delivery", color = Color(0xFF94A3B8))
                Text("${ops.onTimeDeliveryRatePercent}%", color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun VendorFinancialsCard(fin: VendorPortalFinancialSummaryDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Financial Overview", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pending Invoices", color = Color(0xFF94A3B8))
                Text("${fin.pendingInvoices}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Outstanding", color = Color(0xFF94A3B8))
                Text("${fin.totalOutstandingPayables} ${fin.currency}", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Settlements", color = Color(0xFF94A3B8))
                Text("${fin.totalSettlements}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun VendorQualityCard(q: VendorPortalQualitySummaryDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quality & Rejections", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inspections Passed", color = Color(0xFF94A3B8))
                Text("${q.passedInspections}/${q.totalInspections}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Defect Rate", color = Color(0xFF94A3B8))
                Text("${q.overallDefectRatePercent}%", color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Open Disputes", color = Color(0xFF94A3B8))
                Text("${q.openDisputes}", color = if (q.openDisputes > 0) Color(0xFFF87171) else Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun VendorComplianceCard(comp: VendorPortalComplianceSummaryDto, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Compliance Status", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Risk Level", color = Color(0xFF94A3B8))
                Text(comp.complianceRiskLevel, color = Color(0xFF4ADE80), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Active Certifications", color = Color(0xFF94A3B8))
                Text("${comp.activeCertificationsCount}", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tax Status", color = Color(0xFF94A3B8))
                Text(comp.taxComplianceStatus, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
