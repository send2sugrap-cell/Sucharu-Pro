package com.sucharu.sucharupro.ui.features.profitability.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sucharu.sucharupro.data.api.model.profitability.CrossDimensionConcentrationResultDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityIntelligenceConcentrationScreen(
    concentrationResult: CrossDimensionConcentrationResultDto?,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val gold = Color(0xFFFFD166)
    val accentCyan = Color(0xFF9ECAFF)
    val warningOrange = Color(0xFFFFB74D)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Concentration & Dependency", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
                        Text("PARETO CONCENTRATION RATIOS", fontWeight = FontWeight.Bold, color = gold, fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Top 1 Share: ${concentrationResult?.top1Share ?: 0}%", fontSize = 12.sp, color = Color.White)
                            Text("Top 5 Share: ${concentrationResult?.top5Share ?: 0}%", fontSize = 12.sp, color = Color.White)
                            Text("Top 10 Share: ${concentrationResult?.top10Share ?: 0}%", fontSize = 12.sp, color = Color.White)
                        }
                        Text("Dependency Risk: ${concentrationResult?.dependencyLevel ?: "LOW"}", fontSize = 12.sp, color = warningOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            items(concentrationResult?.topEntities ?: emptyList()) { entity ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${entity.rank} ${entity.entityLabel}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Text("৳ ${entity.amount} (${entity.sharePercentage}%)", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
