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
import com.sucharu.sucharupro.data.api.model.profitability.ManagementPriorityItemDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementPriorityScreen(
    priorities: List<ManagementPriorityItemDto>,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val warningOrange = Color(0xFFFFB74D)
    val errorRed = Color(0xFFFF6B6B)
    val gold = Color(0xFFFFD166)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Management Action Queue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
            items(priorities) { prio ->
                val levelColor = when (prio.priorityLevel) {
                    "CRITICAL" -> errorRed
                    "HIGH" -> warningOrange
                    else -> gold
                }

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
                            Text(prio.issueTitle, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Score: ${prio.priorityScore}/100", color = levelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(prio.issueDescription, fontSize = 12.sp, color = Color.LightGray)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Impact: ৳ ${prio.financialImpact}", fontSize = 11.sp, color = errorRed, fontWeight = FontWeight.SemiBold)
                            Text("Action: ${prio.recommendedActionCode.replace('_', ' ')}", fontSize = 11.sp, color = gold)
                            Text("Level: ${prio.priorityLevel}", fontSize = 11.sp, color = levelColor)
                        }
                    }
                }
            }
        }
    }
}
