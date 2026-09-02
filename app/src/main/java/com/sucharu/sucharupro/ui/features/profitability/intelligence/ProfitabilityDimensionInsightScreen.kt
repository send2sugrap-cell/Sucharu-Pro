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
import com.sucharu.sucharupro.data.api.model.profitability.DimensionInsightDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityDimensionInsightScreen(
    dimensions: List<DimensionInsightDto>,
    selectedDimensionType: String? = null,
    onDimensionTypeSelect: (String?) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("6-Dimension Profitability Insights", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
            items(dimensions) { dim ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dim.dimensionLabel, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text(dim.dimensionType, color = accentCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Revenue", fontSize = 11.sp, color = Color.Gray)
                                Text("৳ ${dim.revenue}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Cost", fontSize = 11.sp, color = Color.Gray)
                                Text("৳ ${dim.cost}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Gross Profit", fontSize = 11.sp, color = Color.Gray)
                                val pColor = if (dim.grossProfit >= BigDecimal.ZERO) successGreen else errorRed
                                Text("৳ ${dim.grossProfit}", fontSize = 13.sp, color = pColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Margin: ${dim.margin ?: "N/A"}%", fontSize = 11.sp, color = accentCyan)
                            Text("Share: ${dim.shareOfRevenue}% Rev / ${dim.shareOfCost}% Cost", fontSize = 11.sp, color = Color.LightGray)
                            Text("Risk: ${dim.riskLevel}", fontSize = 11.sp, color = if (dim.riskLevel == "LOW") successGreen else errorRed)
                        }
                    }
                }
            }
        }
    }
}
