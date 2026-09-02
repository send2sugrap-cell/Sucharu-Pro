package com.sucharu.sucharupro.ui.features.internalcommunication

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationPriority
import com.sucharu.sucharupro.domain.model.communication.internal.InternalCommunicationRecipientType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalCommunicationBroadcastScreen(
    viewModel: InternalCommunicationBroadcastViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedScope by remember { mutableStateOf(InternalCommunicationRecipientType.ALL_INTERNAL_USERS) }
    var selectedPriority by remember { mutableStateOf(InternalCommunicationPriority.HIGH) }
    var requiresAck by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Internal Broadcast Manager", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 03 • Organization-Wide Alerts", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Broadcast Scope: ${selectedScope.defaultLabel}", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Broadcast Subject", color = Color(0xFF94A3B8)) },
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
                            label = { Text("Broadcast Directive Body", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color(0xFFF8FAFC),
                                unfocusedTextColor = Color(0xFFF8FAFC)
                            )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Require Acknowledgement", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                            Switch(
                                checked = requiresAck,
                                onCheckedChange = { requiresAck = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFC084FC), checkedTrackColor = Color(0xFFC084FC).copy(alpha = 0.3f))
                            )
                        }

                        Button(
                            onClick = {
                                if (subject.isNotBlank() && message.isNotBlank()) {
                                    viewModel.broadcast(
                                        recipientType = selectedScope,
                                        priority = selectedPriority,
                                        subject = subject,
                                        message = message,
                                        requiresAcknowledgement = requiresAck,
                                        onSuccess = onNavigateBack
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transmit Broadcast", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
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
