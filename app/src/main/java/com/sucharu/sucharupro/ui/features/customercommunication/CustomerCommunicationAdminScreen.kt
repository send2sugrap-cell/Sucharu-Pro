package com.sucharu.sucharupro.ui.features.customercommunication

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.customer.CustomerCommunicationType
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCommunicationAdminScreen(
    viewModel: CustomerCommunicationAdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var customerId by remember { mutableStateOf("CUST-001") }
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CustomerCommunicationType.GENERAL_MESSAGE) }
    var selectedPriority by remember { mutableStateOf(NotificationPriority.NORMAL) }
    var selectedChannel by remember { mutableStateOf(NotificationChannel.IN_APP) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Customer Communications Admin", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 02 • Broadcast & Direct Messaging", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFF8FAFC))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadData() }) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Metrics Overview
            if (summary != null) {
                item {
                    Text("Overall Communication Volume", color = Color(0xFFF8FAFC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Total Dispatches", summary.totalCount.toString(), accentColor = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Unread Alerts", summary.unreadCount.toString(), accentColor = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                    }
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CustomerEngagementMetricCard("Delivered", summary.deliveredCount.toString(), accentColor = Color(0xFF34D399), modifier = Modifier.weight(1f))
                        CustomerEngagementMetricCard("Acknowledged", summary.acknowledgedCount.toString(), accentColor = Color(0xFFC084FC), modifier = Modifier.weight(1f))
                    }
                }
            }

            // Create Dispatch Form
            item {
                Text("Compose Customer Communication", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customerId,
                            onValueChange = { customerId = it },
                            label = { Text("Target Customer ID", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Notice Title", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message Body", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        Button(
                            onClick = {
                                if (title.isNotBlank() && message.isNotBlank() && customerId.isNotBlank()) {
                                    viewModel.sendCommunication(
                                        customerId = customerId,
                                        type = selectedType,
                                        channel = selectedChannel,
                                        priority = selectedPriority,
                                        title = title,
                                        message = message
                                    )
                                    title = ""
                                    message = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dispatch Communication", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }

                        if (state.successMessage != null) {
                            Text(state.successMessage ?: "", color = Color(0xFF34D399), fontSize = 12.sp)
                        }
                        if (state.errorMessage != null) {
                            Text(state.errorMessage ?: "", color = Color(0xFFF87171), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
