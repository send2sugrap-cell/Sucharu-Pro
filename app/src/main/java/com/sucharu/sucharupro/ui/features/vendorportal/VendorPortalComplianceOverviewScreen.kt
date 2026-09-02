package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalComplianceOverviewDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalComplianceOverviewScreen(
    overview: VendorPortalComplianceOverviewDto,
    onViewRequirementsClick: () -> Unit = {},
    onViewRecordsClick: () -> Unit = {},
    onViewExpiriesClick: () -> Unit = {},
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
                        text = "Vendor Compliance Overview",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Compliance Rate", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                Text(
                                    "${String.format("%.1f", overview.complianceRate)}%",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Risk: ${overview.overallRiskLevel}",
                                    color = if (overview.overallRiskLevel == "LOW") Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Status: ${overview.overallComplianceStatus}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Divider(color = Color(0xFF334155))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Compliant", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${overview.compliantCount}", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Pending", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${overview.pendingCount}", color = Color(0xFFF59E0B), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Non-Compliant", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${overview.nonCompliantCount}", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Expired", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${overview.expiredCertificationsCount}", color = Color(0xFFEF4444), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onViewRequirementsClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Requirements (${overview.totalRequirements})", color = Color(0xFF38BDF8))
                    }
                    Button(
                        onClick = onViewExpiriesClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Expiries (${overview.upcomingExpiringCertificationsCount})", color = Color(0xFFF59E0B))
                    }
                }
            }
        }
    }
}
