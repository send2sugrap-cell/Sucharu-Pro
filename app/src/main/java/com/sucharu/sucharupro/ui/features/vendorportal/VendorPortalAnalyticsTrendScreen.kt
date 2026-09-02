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
import com.sucharu.sucharupro.data.api.model.VendorPortalTrendMetricDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalAnalyticsTrendScreen(
    trends: List<VendorPortalTrendMetricDto>,
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
                        text = "Vendor Trend Projections",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(trends) { trend ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                text = trend.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Badge(
                                containerColor = when (trend.direction) {
                                    "IMPROVING" -> Color(0xFF10B981)
                                    "DECLINING" -> Color(0xFFEF4444)
                                    else -> Color(0xFF64748B)
                                }
                            ) {
                                Text(
                                    text = trend.direction,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Current",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${trend.currentValue} ${trend.unit}".trim(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = "Previous",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${trend.previousValue} ${trend.unit}".trim(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Change",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "${if (trend.delta > 0) "+" else ""}${trend.delta} (${if (trend.percentageDelta > 0) "+" else ""}${trend.percentageDelta}%)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (trend.direction) {
                                        "IMPROVING" -> Color(0xFF34D399)
                                        "DECLINING" -> Color(0xFFF87171)
                                        else -> Color(0xFF94A3B8)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
