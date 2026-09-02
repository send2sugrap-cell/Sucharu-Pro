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
import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityRelationshipInsightDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityRelationshipScreen(
    relationships: List<ProfitabilityRelationshipInsightDto>,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val gold = Color(0xFFFFD166)
    val accentCyan = Color(0xFF9ECAFF)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Cross-Dimensional Relationships", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
            items(relationships) { rel ->
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
                            Text(
                                "${rel.fromEntityLabel} ➔ ${rel.toEntityLabel}",
                                fontWeight = FontWeight.Bold,
                                color = gold,
                                fontSize = 14.sp
                            )
                            Text("${rel.fromDimensionType} ➔ ${rel.toDimensionType}", color = accentCyan, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Revenue", fontSize = 11.sp, color = Color.Gray)
                                Text("৳ ${rel.revenue}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Cost", fontSize = 11.sp, color = Color.Gray)
                                Text("৳ ${rel.cost}", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Gross Profit", fontSize = 11.sp, color = Color.Gray)
                                val pColor = if (rel.grossProfit >= BigDecimal.ZERO) successGreen else errorRed
                                Text("৳ ${rel.grossProfit}", fontSize = 13.sp, color = pColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Margin: ${rel.grossMargin ?: "N/A"}%", fontSize = 11.sp, color = accentCyan)
                            Text("Class: ${rel.classification}", fontSize = 11.sp, color = Color.LightGray)
                            Text("Integrity: ${rel.sourceIntegrityStatus}", fontSize = 11.sp, color = successGreen)
                        }
                    }
                }
            }
        }
    }
}
