package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.sucharu.sucharupro.data.api.model.VendorPortalPerformanceScorecardSummaryDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalScorecardDetailsScreen(
    scorecard: VendorPortalPerformanceScorecardSummaryDto,
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scorecard Details",
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scorecard #${scorecard.scorecardId.take(12)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${String.format("%.1f", scorecard.overallScore)}%",
                                color = Color(0xFF38BDF8),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Period: ${dateFormat.format(Date(scorecard.periodStart))} to ${dateFormat.format(Date(scorecard.periodEnd))} (${scorecard.periodType})",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Rating: ${scorecard.rating} • Risk Level: ${scorecard.riskLevel} • Status: ${scorecard.status}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                        if (!scorecard.notes.isNullOrBlank()) {
                            Text(
                                text = "Notes: ${scorecard.notes}",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Scorecard KPI Breakdown",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(scorecard.items) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${String.format("%.1f", item.normalizedScore)}%",
                                color = if (item.normalizedScore >= 80.0) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target: ${item.targetValue} ${item.unit} • Actual: ${item.actualValue} ${item.unit}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Weighted: ${String.format("%.1f", item.weightedScore)}",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
