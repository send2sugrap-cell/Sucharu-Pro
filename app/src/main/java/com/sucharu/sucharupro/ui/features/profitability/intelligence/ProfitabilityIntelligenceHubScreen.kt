package com.sucharu.sucharupro.ui.features.profitability.intelligence

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityIntelligenceSnapshotDto
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityIntelligenceHubScreen(
    snapshot: ProfitabilityIntelligenceSnapshotDto?,
    onNavigateToDimensions: () -> Unit = {},
    onNavigateToRelationships: () -> Unit = {},
    onNavigateToDrivers: () -> Unit = {},
    onNavigateToLeakages: () -> Unit = {},
    onNavigateToPriorities: () -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToTrends: () -> Unit = {},
    onNavigateToRankings: () -> Unit = {},
    onNavigateToConcentration: () -> Unit = {},
    onNavigateToProvenance: () -> Unit = {},
    onNavigateToReconciliation: () -> Unit = {},
    onNavigateToAudit: () -> Unit = {},
    onRecalculateClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val accentCyan = Color(0xFF9ECAFF)
    val gold = Color(0xFFFFD166)
    val successGreen = Color(0xFF4EBA6F)
    val warningOrange = Color(0xFFFFB74D)
    val errorRed = Color(0xFFFF6B6B)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profitability Intelligence Hub",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Cross-Dimensional Decision Engine • Module 16 Step 07",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onRecalculateClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate", tint = Color.White)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Executive Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXECUTIVE PROFITABILITY CORE",
                                fontWeight = FontWeight.Bold,
                                color = gold,
                                fontSize = 14.sp
                            )
                            val healthColor = when (snapshot?.healthStatus) {
                                "EXCELLENT", "HEALTHY" -> successGreen
                                "MODERATE" -> warningOrange
                                else -> errorRed
                            }
                            Text(
                                text = snapshot?.healthStatus ?: "HEALTHY",
                                color = healthColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Recognized Revenue", fontSize = 12.sp, color = Color.Gray)
                                Text("৳ ${snapshot?.revenue ?: BigDecimal.ZERO}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Total Cost", fontSize = 12.sp, color = Color.Gray)
                                Text("৳ ${snapshot?.totalCost ?: BigDecimal.ZERO}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Gross Profit", fontSize = 12.sp, color = Color.Gray)
                                val pColor = if ((snapshot?.grossProfit ?: BigDecimal.ZERO) >= BigDecimal.ZERO) successGreen else errorRed
                                Text("৳ ${snapshot?.grossProfit ?: BigDecimal.ZERO}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gross Margin: ${snapshot?.grossMargin ?: "N/A"}%", fontSize = 12.sp, color = accentCyan)
                            Text("Health Score: ${snapshot?.healthScore?.overallScore ?: "N/A"}/100", fontSize = 12.sp, color = gold)
                            Text("Integrity: VERIFIED", fontSize = 12.sp, color = successGreen)
                        }
                    }
                }
            }

            // Navigation Grid to Analytical Intelligence Screens
            item {
                Text(
                    text = "ANALYTICAL INTELLIGENCE CAPABILITIES",
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "6-Dimension Insights",
                        subtitle = "${snapshot?.dimensionCount ?: 0} Entities Tracked",
                        icon = Icons.Default.Category,
                        tint = accentCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDimensions
                    )
                    IntelligenceNavCard(
                        title = "Relationships",
                        subtitle = "${snapshot?.relationshipCount ?: 0} Matrix Pairs",
                        icon = Icons.Default.CompareArrows,
                        tint = gold,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRelationships
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "Profit Drivers",
                        subtitle = "${snapshot?.driverCount ?: 0} Drivers Detected",
                        icon = Icons.Default.TrendingUp,
                        tint = successGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDrivers
                    )
                    IntelligenceNavCard(
                        title = "Profit Leakages",
                        subtitle = "${snapshot?.leakageCount ?: 0} Leakages Flagged",
                        icon = Icons.Default.Warning,
                        tint = errorRed,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToLeakages
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "Priority Queue",
                        subtitle = "${snapshot?.priorityCount ?: 0} Action Items",
                        icon = Icons.Default.Assignment,
                        tint = warningOrange,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPriorities
                    )
                    IntelligenceNavCard(
                        title = "Health Score",
                        subtitle = "Composite 0-100 Score",
                        icon = Icons.Default.HealthAndSafety,
                        tint = accentCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHealth
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "Rankings",
                        subtitle = "Multi-Criteria Sort",
                        icon = Icons.Default.FormatListNumbered,
                        tint = gold,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToRankings
                    )
                    IntelligenceNavCard(
                        title = "Concentration",
                        subtitle = "Dependency & Pareto",
                        icon = Icons.Default.PieChart,
                        tint = accentCyan,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToConcentration
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "Period Trends",
                        subtitle = "Period Variance",
                        icon = Icons.Default.Timeline,
                        tint = successGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToTrends
                    )
                    IntelligenceNavCard(
                        title = "Reconciliation",
                        subtitle = "Non-Mutating Balance",
                        icon = Icons.Default.CheckCircle,
                        tint = successGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToReconciliation
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntelligenceNavCard(
                        title = "Provenance Trail",
                        subtitle = "SHA-256 Fingerprints",
                        icon = Icons.Default.Fingerprint,
                        tint = gold,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProvenance
                    )
                    IntelligenceNavCard(
                        title = "Audit History",
                        subtitle = "Decision Event Log",
                        icon = Icons.Default.History,
                        tint = Color.LightGray,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToAudit
                    )
                }
            }
        }
    }
}

@Composable
fun IntelligenceNavCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardBg = Color(0xFF1C2541)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Text(subtitle, fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}
