package com.sucharu.sucharupro.ui.features.internalcommunication

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalCommunicationDetailsScreen(
    communicationId: String,
    viewModel: InternalCommunicationDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToThread: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(communicationId) {
        viewModel.loadDetails(communicationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Details", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
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
        val comm = state.communication

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF38BDF8))
            }
        } else if (comm == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(state.errorMessage ?: "Message not found.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(comm.communicationNo, color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    InternalCommunicationPriorityBadge(comm.priority)
                                    InternalCommunicationStatusBadge(comm.status)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(comm.subject, color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(comm.message, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 18.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("From: ${comm.senderUserId} (${comm.senderRole.name})", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(formatTime(comm.createdAt), color = Color(0xFF64748B), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Thread Action
                val threadId = comm.threadId
                if (threadId != null && onNavigateToThread != null) {
                    item {
                        OutlinedButton(
                            onClick = { onNavigateToThread(threadId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38BDF8)))
                        ) {
                            Text("Open Conversation Thread ($threadId)")
                        }
                    }
                }

                // Acknowledgement Section
                if (comm.requiresAcknowledgement && !comm.isAcknowledged) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFC084FC).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Acknowledgement Required", color = Color(0xFFC084FC), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Please acknowledge this urgent or critical internal directive.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { viewModel.acknowledge(comm.communicationId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC084FC)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0F172A))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Acknowledge & Confirm Receipt", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Activity History
                if (state.history.isNotEmpty()) {
                    item {
                        Text("Audit Trail", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    items(state.history, key = { it.eventId }) { h ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF334155).copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(h.eventType.replace('_', ' '), color = Color(0xFFF8FAFC), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    val notes = h.notes
                                    if (!notes.isNullOrBlank()) {
                                        Text(notes, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                    }
                                }
                                Text(formatTime(h.timestamp), color = Color(0xFF64748B), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
