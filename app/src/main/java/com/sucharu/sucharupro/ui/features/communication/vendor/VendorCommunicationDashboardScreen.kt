package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
fun VendorCommunicationDashboardScreen(
    viewModel: VendorCommunicationDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCenter: () -> Unit,
    onNavigateToCompose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor & Supplier Communications", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Supplier Engagement Hub", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCompose,
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF0F172A)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Vendor Communication")
            }
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Vendor Communication Overview", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // Summary metrics
                state.summary?.let { summary ->
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VendorCommunicationSummaryCard("Total", summary.totalCount, Color(0xFF38BDF8), Modifier.weight(1f))
                            VendorCommunicationSummaryCard("Unread", summary.unreadCount, Color(0xFFFBBF24), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VendorCommunicationSummaryCard("Acknowledged", summary.acknowledgedCount, Color(0xFF4ADE80), Modifier.weight(1f))
                            VendorCommunicationSummaryCard("Pending Ack", summary.pendingAcknowledgementCount, Color(0xFFC084FC), Modifier.weight(1f))
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            VendorCommunicationSummaryCard("Sent", summary.sentCount, Color(0xFF60A5FA), Modifier.weight(1f))
                            VendorCommunicationSummaryCard("Failed", summary.failedCount, Color(0xFFF87171), Modifier.weight(1f))
                        }
                    }
                }

                // Engagement summary
                state.engagementSummary?.let { eng ->
                    item {
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
                        Text("Engagement Analytics", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                VendorEngagementStatRow("Read Rate", "%.1f%%".format(eng.readRate), Color(0xFF4ADE80))
                                VendorEngagementStatRow("Ack Rate", "%.1f%%".format(eng.acknowledgementRate), Color(0xFF38BDF8))
                                VendorEngagementStatRow("Recent Activity", "${eng.recentActivityCount}", Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Communications Center", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToCenter) {
                            Text("View All", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                    }
                }

                if (state.recentCommunications.isEmpty()) {
                    item { EmptyVendorCommunicationState("No vendor communications yet. Start by composing a message.") }
                } else {
                    items(state.recentCommunications, key = { it.communicationId }) { comm ->
                        VendorCommunicationCard(communication = comm, onClick = {})
                    }
                }

                state.error?.let { err ->
                    item { Text(err, color = Color(0xFFF87171), fontSize = 13.sp) }
                }
            }
        }
    }
}
