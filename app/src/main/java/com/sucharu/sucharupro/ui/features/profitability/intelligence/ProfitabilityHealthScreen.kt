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
import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityHealthScoreDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityHealthScreen(
    healthScore: ProfitabilityHealthScoreDto?,
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
                    Text("Profitability Health Score", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("COMPOSITE HEALTH SCORE", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${healthScore?.overallScore ?: "0.0000"} / 100", fontSize = 32.sp, color = gold, fontWeight = FontWeight.Bold)
                        Text("Status: ${healthScore?.healthLevel ?: "HEALTHY"}", fontSize = 14.sp, color = successGreen, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(healthScore?.explanation ?: "Deterministic score", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("HEALTH PILLARS BREAKDOWN", fontWeight = FontWeight.Bold, color = accentCyan, fontSize = 13.sp)
                        ScoreRow("Margin Score (25%)", "${healthScore?.marginScore ?: 0}/100")
                        ScoreRow("Trend Score (15%)", "${healthScore?.trendScore ?: 0}/100")
                        ScoreRow("Cost Stability (15%)", "${healthScore?.costStabilityScore ?: 0}/100")
                        ScoreRow("Revenue Stability (15%)", "${healthScore?.revenueStabilityScore ?: 0}/100")
                        ScoreRow("Concentration Health (10%)", "${healthScore?.concentrationScore ?: 0}/100")
                        ScoreRow("Vendor Dependency (10%)", "${healthScore?.vendorDependencyScore ?: 0}/100")
                        ScoreRow("Data Integrity (10%)", "${healthScore?.dataIntegrityScore ?: 0}/100")
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.LightGray)
        Text(score, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
