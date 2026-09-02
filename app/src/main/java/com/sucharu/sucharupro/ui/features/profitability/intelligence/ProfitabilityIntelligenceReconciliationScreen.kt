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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityIntelligenceReconciliationEventDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityIntelligenceReconciliationScreen(
    reconciliationEvents: List<ProfitabilityIntelligenceReconciliationEventDto>,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val successGreen = Color(0xFF4EBA6F)
    val errorRed = Color(0xFFFF6B6B)
    val gold = Color(0xFFFFD166)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Intelligence Reconciliation", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
            items(reconciliationEvents) { evt ->
                val statusColor = if (evt.isBalanced) successGreen else errorRed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reconciliation Event", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(if (evt.isBalanced) "BALANCED" else "DISCREPANCY", color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("Profit Diff: ৳ ${evt.profitDifference} • Revenue Diff: ৳ ${evt.revenueDifference}", fontSize = 11.sp, color = Color.LightGray)

                        if (evt.assertions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Assertions Checked (${evt.assertions.size}):", fontSize = 11.sp, color = gold, fontWeight = FontWeight.SemiBold)
                            evt.assertions.forEach { a ->
                                Text("• ${a.assertionName}: ${if (a.isPassed) "PASSED" else "FAILED"}", fontSize = 10.sp, color = if (a.isPassed) successGreen else errorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
