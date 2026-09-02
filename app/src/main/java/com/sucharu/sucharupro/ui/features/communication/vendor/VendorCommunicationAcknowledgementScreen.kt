package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun VendorCommunicationAcknowledgementScreen(
    communicationId: String,
    viewModel: VendorCommunicationAcknowledgementViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(communicationId) { viewModel.loadCommunication(communicationId) }
    val state by viewModel.uiState.collectAsState()
    var responseMessage by remember { mutableStateOf("") }

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Acknowledge Communication", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Vendor Response", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Communication preview
            state.communication?.let { comm ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VendorCommunicationTypeChip(type = comm.communicationType)
                            VendorCommunicationPriorityChip(priority = comm.priority)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(comm.subject, color = Color(0xFFF8FAFC), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(comm.message, color = Color(0xFF94A3B8), fontSize = 13.sp)
                    }
                }
            }

            // Already acknowledged
            if (state.acknowledgement != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF4ADE80).copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80))
                        Column {
                            Text("Already ${state.acknowledgement!!.status.name.lowercase().replaceFirstChar { it.uppercase() }}", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                            state.acknowledgement!!.message?.let { Text(it, color = Color(0xFF94A3B8), fontSize = 12.sp) }
                        }
                    }
                }
            } else {
                // Response message
                OutlinedTextField(
                    value = responseMessage,
                    onValueChange = { responseMessage = it },
                    label = { Text("Response Note (optional)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color(0xFFF8FAFC), unfocusedTextColor = Color(0xFFF8FAFC), cursorColor = Color(0xFF38BDF8)
                    ),
                    maxLines = 5
                )

                state.error?.let { Text(it, color = Color(0xFFF87171), fontSize = 13.sp) }

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Acknowledge
                    Button(
                        onClick = { viewModel.acknowledge(communicationId, responseMessage.ifBlank { null }) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4ADE80)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A))
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Acknowledge", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Decline
                    OutlinedButton(
                        onClick = { viewModel.decline(communicationId, responseMessage.ifBlank { null }) },
                        enabled = !state.isSubmitting,
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF87171))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Decline", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
