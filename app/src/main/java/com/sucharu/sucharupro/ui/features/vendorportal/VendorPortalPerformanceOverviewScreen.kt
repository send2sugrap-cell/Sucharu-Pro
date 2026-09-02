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
import com.sucharu.sucharupro.data.api.model.VendorPortalPerformanceOverviewDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalPerformanceOverviewScreen(
    overview: VendorPortalPerformanceOverviewDto,
    onViewScorecardsClick: () -> Unit = {},
    onViewTrendsClick: () -> Unit = {},
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
                        text = "Vendor Performance Overview",
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
                                Text("Overall Performance Score", color = Color(0xFF94A3B8), fontSize = 14.sp)
                                Text(
                                    "${String.format("%.1f", overview.overallScore)}%",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Rating: ${overview.rating}",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Risk: ${overview.riskLevel}",
                                    color = if (overview.riskLevel == "LOW") Color(0xFF10B981) else Color(0xFFEF4444),
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
                                Text("On-Time Delivery", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${String.format("%.1f", overview.onTimeDeliveryRate)}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("PO Fulfillment", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${String.format("%.1f", overview.poFulfillmentRate)}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Defect Rate", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text("${String.format("%.2f", overview.defectRate)}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Quality Rating", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                Text(overview.qualityRating, color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Strengths
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Top Strengths", color = Color(0xFF10B981), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (overview.topStrengths.isEmpty()) {
                            Text("No specific high-score categories recorded.", color = Color(0xFF64748B), fontSize = 13.sp)
                        } else {
                            overview.topStrengths.forEach { str ->
                                Text("✔ $str", color = Color(0xFFE2E8F0), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Improvement Areas
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Areas for Improvement", color = Color(0xFFF59E0B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (overview.improvementAreas.isEmpty()) {
                            Text("No low-score warning areas recorded.", color = Color(0xFF64748B), fontSize = 13.sp)
                        } else {
                            overview.improvementAreas.forEach { imp ->
                                Text("⚠ $imp", color = Color(0xFFE2E8F0), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
