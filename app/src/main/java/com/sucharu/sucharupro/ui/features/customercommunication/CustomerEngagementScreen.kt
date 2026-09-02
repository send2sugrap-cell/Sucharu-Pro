package com.sucharu.sucharupro.ui.features.customercommunication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
fun CustomerEngagementScreen(
    viewModel: CustomerEngagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customer Engagement Metrics", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 02 • Telemetry & Analytics Foundation", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadEngagement() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        val summary = state.summary

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (summary == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No engagement telemetry available.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Interaction & Consumption Rates", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Read Rate", "${summary.readRatePercent}%", accentColor = Color(0xFF34D399), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Ack Rate", "${summary.acknowledgementRatePercent}%", accentColor = Color(0xFFC084FC), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Total Dispatched", summary.messagesSent.toString(), accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Total Delivered", summary.messagesDelivered.toString(), accentColor = Color(0xFF60A5FA), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Messages Read", summary.messagesRead.toString(), accentColor = Color(0xFF10B981), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Acknowledged", summary.messagesAcknowledged.toString(), accentColor = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Offer Views", summary.offerViews.toString(), accentColor = Color(0xFFF472B6), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Notice Views", summary.announcementViews.toString(), accentColor = Color(0xFFFB923C), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
