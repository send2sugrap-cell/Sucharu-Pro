package com.sucharu.sucharupro.ui.features.finance.analytics

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAnalyticsSnapshotScreen(
    viewModel: FinanceAnalyticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snapshots = state.snapshots

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Analytics Snapshots", color = Color(0xFFF8FAFC), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.createSnapshot() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture Snapshot", tint = Color(0xFF34D399))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (snapshots.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No analytics snapshots saved yet. Tap camera to freeze current audit state.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Immutable Audit Snapshots (${snapshots.size})", color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                items(snapshots) { snap ->
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Snapshot #${snap.snapshotId.take(8)}", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Score: ${snap.healthScore} (${snap.healthStatus.defaultLabel})", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Period: ${snap.periodLabel} • Rev: ${snap.totalRevenue.formatted()} • Profit: ${snap.netProfit.formatted()}", color = Color(0xFFF8FAFC), fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("SHA-256: ${snap.snapshotHash.take(24)}...", color = Color(0xFF64748B), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
