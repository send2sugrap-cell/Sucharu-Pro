package com.sucharu.sucharupro.ui.features.profitability.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.CrossDimensionTrendResultDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityIntelligenceTrendScreen(
    trendResult: CrossDimensionTrendResultDto?,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val successGreen = Color(0xFF4EBA6F)
    val gold = Color(0xFFFFD166)
    val accentCyan = Color(0xFF9ECAFF)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Period Variance & Trends", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkNavyBg)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(darkNavyBg)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("PERIOD VARIANCE SUMMARY", fontWeight = FontWeight.Bold, color = gold, fontSize = 13.sp)
                        Text(trendResult?.explanation ?: "Trend analysis", fontSize = 12.sp, color = Color.LightGray)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Revenue Delta: ৳ ${trendResult?.revenueDelta ?: 0} (${trendResult?.revenueDeltaPct ?: 0}%)", fontSize = 12.sp, color = Color.White)
                            Text("Cost Delta: ৳ ${trendResult?.costDelta ?: 0} (${trendResult?.costDeltaPct ?: 0}%)", fontSize = 12.sp, color = Color.White)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Profit Delta: ৳ ${trendResult?.profitDelta ?: 0} (${trendResult?.profitDeltaPct ?: 0}%)", fontSize = 12.sp, color = successGreen, fontWeight = FontWeight.Bold)
                            Text("Trend: ${trendResult?.trendDirection ?: "STABLE"}", fontSize = 12.sp, color = accentCyan)
                        }
                    }
                }
            }
        }
    }
}
