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
fun VendorCommunicationDetailsScreen(
    communicationId: String,
    viewModel: VendorCommunicationDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (String) -> Unit,
    onNavigateToAcknowledge: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(communicationId) { viewModel.loadCommunication(communicationId) }
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.communication?.communicationNo ?: "Vendor Communication", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Details", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadCommunication(communicationId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
            state.error != null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.error ?: "", color = Color(0xFFF87171))
            }
            state.communication == null -> Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Communication not found.", color = Color(0xFF94A3B8))
            }
            else -> {
                val comm = state.communication!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Status & badges
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VendorCommunicationStatusChip(status = comm.status)
                            VendorCommunicationTypeChip(type = comm.communicationType)
                            VendorCommunicationPriorityChip(priority = comm.priority)
                        }
                    }

                    // Subject & message
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(comm.subject, color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(comm.message, color = Color(0xFF94A3B8), fontSize = 14.sp)
                            }
                        }
                    }

                    // Metadata
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Communication Details", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                VendorEngagementStatRow("Vendor ID", comm.vendorId, Color(0xFFF8FAFC))
                                comm.referenceType?.let { VendorEngagementStatRow("Reference", "$it / ${comm.referenceId ?: "—"}", Color(0xFF94A3B8)) }
                                if (comm.requiresAcknowledgement) {
                                    VendorEngagementStatRow("Acknowledgement", if (comm.isAcknowledged) "✓ Acknowledged" else if (comm.isDeclined) "✗ Declined" else "Pending", if (comm.isAcknowledged) Color(0xFF4ADE80) else if (comm.isDeclined) Color(0xFFF87171) else Color(0xFFFBBF24))
                                }
                                state.acknowledgement?.let { ack ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Ack Note: ${ack.message ?: "—"}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Actions
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (comm.requiresAcknowledgement && !comm.isAcknowledged && !comm.isDeclined && !comm.isCancelled) {
                                Button(
                                    onClick = { onNavigateToAcknowledge(communicationId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Acknowledge", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                            }
                            OutlinedButton(
                                onClick = { onNavigateToHistory(communicationId) },
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View History", color = Color(0xFF38BDF8))
                            }
                        }
                    }

                    // History preview
                    if (state.history.isNotEmpty()) {
                        item { Text("Recent Activity", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                        items(state.history.takeLast(5)) { h -> VendorCommunicationHistoryRow(h) }
                    }
                }
            }
        }
    }
}
