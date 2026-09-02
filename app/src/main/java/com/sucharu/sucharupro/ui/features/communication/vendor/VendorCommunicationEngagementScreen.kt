package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun VendorCommunicationEngagementScreen(
    vendorId: String,
    viewModel: VendorCommunicationEngagementViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(vendorId) { viewModel.loadEngagement(vendorId) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor Engagement Analytics", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Vendor: $vendorId", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadEngagement(vendorId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
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
                // Engagement summary
                state.summary?.let { eng ->
                    item { Text("Engagement Summary", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                VendorEngagementStatRow("Read Count", "${eng.readCount}", Color(0xFF4ADE80))
                                VendorEngagementStatRow("Acknowledged", "${eng.acknowledgedCount}", Color(0xFF38BDF8))
                                VendorEngagementStatRow("Read Rate", "%.1f%%".format(eng.readRate), Color(0xFF4ADE80))
                                VendorEngagementStatRow("Ack Rate", "%.1f%%".format(eng.acknowledgementRate), Color(0xFF38BDF8))
                                VendorEngagementStatRow("Recent Activity (7d)", "${eng.recentActivityCount}", Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                // Event timeline
                if (state.events.isNotEmpty()) {
                    item { Text("Engagement Events (${state.events.size})", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    items(state.events, key = { it.eventId }) { event ->
                        VendorCommunicationEngagementEventRow(event)
                    }
                } else {
                    item { EmptyVendorCommunicationState("No engagement events recorded for this vendor.") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCommunicationAdminScreen(
    viewModel: VendorCommunicationListViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Vendor Communication Admin", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Admin Console", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCommunications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("All Communications — Admin View", color = Color(0xFF94A3B8), fontSize = 12.sp) }

            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                }
            } else if (state.communications.isEmpty()) {
                item { EmptyVendorCommunicationState("No communications to manage.") }
            } else {
                items(state.communications, key = { it.communicationId }) { comm ->
                    VendorCommunicationCard(communication = comm, onClick = { onNavigateToDetails(comm.communicationId) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCommunicationScheduleScreen(
    viewModel: VendorCommunicationListViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scheduled = state.communications.filter { it.status == com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationStatus.SCHEDULED }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Scheduled Communications", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Scheduled Queue", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (scheduled.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyVendorCommunicationState("No scheduled communications.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(scheduled, key = { it.communicationId }) { comm ->
                    VendorCommunicationCard(communication = comm, onClick = {})
                }
            }
        }
    }
}
