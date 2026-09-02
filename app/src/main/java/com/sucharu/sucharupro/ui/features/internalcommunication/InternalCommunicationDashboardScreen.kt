package com.sucharu.sucharupro.ui.features.internalcommunication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
fun InternalCommunicationDashboardScreen(
    viewModel: InternalCommunicationDashboardViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToCompose: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Internal Staff & Team Communication", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 03 • Operational Chat & Notices", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
                Icon(Icons.Default.Add, contentDescription = "New Message")
            }
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
                Text("No communications available.", color = Color(0xFF94A3B8))
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
                    Text("Communication Metrics", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InternalCommunicationSummaryCard("Total Messages", summary.totalMessages, accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        InternalCommunicationSummaryCard("Unread Messages", summary.unreadMessages, accentColor = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InternalCommunicationSummaryCard("Urgent / Critical", summary.urgentMessages + summary.criticalMessages, accentColor = Color(0xFFF87171), modifier = Modifier.weight(1f))
                        InternalCommunicationSummaryCard("Pending Acks", summary.pendingAcknowledgements, accentColor = Color(0xFFC084FC), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        InternalCommunicationSummaryCard("Team Chats", summary.teamMessages, accentColor = Color(0xFF60A5FA), modifier = Modifier.weight(1f))
                        InternalCommunicationSummaryCard("Dept Notices", summary.departmentMessages, accentColor = Color(0xFF818CF8), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Messages", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToInbox) {
                            Text("Open Inbox", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(state.recentCommunications, key = { it.communicationId }) { comm ->
                    InternalCommunicationCard(
                        communication = comm,
                        onClick = { onNavigateToDetails(comm.communicationId) }
                    )
                }
            }
        }
    }
}
