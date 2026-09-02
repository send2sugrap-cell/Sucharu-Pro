package com.sucharu.sucharupro.ui.features.finance.periodclose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ClosingSnapshotScreen(
    periodId: String,
    viewModel: AccountingPeriodViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.detailsState.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())

    LaunchedEffect(periodId) {
        viewModel.loadPeriodDetails(periodId)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Historical Closing Snapshot (Immutable)",
                        color = Color(0xFFF8FAFC),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
            } else {
                val snapshot = state.snapshot
                if (snapshot == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No closing snapshot generated for this period yet.", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Snapshot Header Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(snapshot.snapshotNo, color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        Surface(
                                            color = Color(0xFF064E3B),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("LOCKED & VERIFIED", color = Color(0xFF6EE7B7), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    }

                                    Text("Period: ${snapshot.periodName}", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Closed by: ${snapshot.generatedBy} on ${dateFormat.format(Date(snapshot.generatedAt))}", color = Color(0xFF94A3B8), fontSize = 11.sp)

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("CRYPTOGRAPHIC INTEGRITY HASH (SHA-256):", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(snapshot.snapshotHash, color = Color(0xFF38BDF8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        // Financial Balances Snapshot Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Verified Closing Balances", color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)

                                    SnapshotBalanceRow("Closing Cash in Hand", snapshot.closingCash.formatted())
                                    SnapshotBalanceRow("Closing Bank Balance", snapshot.closingBank.formatted())
                                    SnapshotBalanceRow("Net Financial Position", snapshot.netFinancialPosition.formatted(), isHighlight = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SnapshotBalanceRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (isHighlight) Color(0xFFF8FAFC) else Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal)
        Text(value, color = if (isHighlight) Color(0xFF10B981) else Color(0xFFCBD5E1), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
