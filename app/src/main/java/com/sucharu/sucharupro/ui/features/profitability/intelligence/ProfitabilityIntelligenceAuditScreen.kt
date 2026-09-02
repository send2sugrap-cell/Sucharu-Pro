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
import com.sucharu.sucharupro.data.api.model.profitability.ProfitabilityIntelligenceAuditEventDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfitabilityIntelligenceAuditScreen(
    auditEvents: List<ProfitabilityIntelligenceAuditEventDto>,
    onBackClick: () -> Unit = {}
) {
    val darkNavyBg = Color(0xFF0B132B)
    val cardBg = Color(0xFF1C2541)
    val gold = Color(0xFFFFD166)
    val successGreen = Color(0xFF4EBA6F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Decision & Calculation Audit Trail", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
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
            items(auditEvents) { evt ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(evt.action, fontWeight = FontWeight.Bold, color = gold, fontSize = 12.sp)
                            Text(evt.resultStatus, color = successGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Actor: ${evt.actorId} (${evt.actorRole}) • Scope: ${evt.scope}", fontSize = 11.sp, color = Color.White)
                        Text("Hash: ${evt.integrityHash ?: "N/A"}", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}
