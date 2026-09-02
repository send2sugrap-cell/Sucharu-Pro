package com.sucharu.sucharupro.ui.features.communication.vendor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
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
import com.sucharu.sucharupro.domain.model.communication.vendor.VendorCommunicationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorCommunicationComposeScreen(
    viewModel: VendorCommunicationComposeViewModel,
    onNavigateBack: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) { viewModel.reset(); onSuccess() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("New Vendor Communication", color = Color(0xFFF8FAFC), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Module 10 Step 05 • Compose", color = Color(0xFF38BDF8), fontSize = 11.sp)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Vendor ID field
            OutlinedTextField(
                value = state.vendorId,
                onValueChange = viewModel::updateVendorId,
                label = { Text("Vendor ID *", color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, tint = Color(0xFF38BDF8)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFFF8FAFC), unfocusedTextColor = Color(0xFFF8FAFC), cursorColor = Color(0xFF38BDF8)
                ),
                singleLine = true
            )

            // Communication type selector
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = state.communicationType.defaultLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Communication Type *", color = Color(0xFF94A3B8)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color(0xFFF8FAFC), unfocusedTextColor = Color(0xFFF8FAFC)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF1E293B))
                ) {
                    VendorCommunicationType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.defaultLabel, color = Color(0xFFF8FAFC), fontSize = 13.sp) },
                            onClick = { viewModel.updateType(type); expanded = false }
                        )
                    }
                }
            }

            // Subject
            OutlinedTextField(
                value = state.subject,
                onValueChange = viewModel::updateSubject,
                label = { Text("Subject *", color = Color(0xFF94A3B8)) },
                leadingIcon = { Icon(Icons.Default.Subject, contentDescription = null, tint = Color(0xFF38BDF8)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFFF8FAFC), unfocusedTextColor = Color(0xFFF8FAFC), cursorColor = Color(0xFF38BDF8)
                ),
                singleLine = true
            )

            // Message
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::updateMessage,
                label = { Text("Message *", color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8), unfocusedBorderColor = Color(0xFF334155),
                    focusedTextColor = Color(0xFFF8FAFC), unfocusedTextColor = Color(0xFFF8FAFC), cursorColor = Color(0xFF38BDF8)
                ),
                maxLines = 8
            )

            state.error?.let { Text(it, color = Color(0xFFF87171), fontSize = 13.sp) }

            Button(
                onClick = { if (!state.isSubmitting) viewModel.sendCommunication() },
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF0F172A))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, tint = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Communication", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

