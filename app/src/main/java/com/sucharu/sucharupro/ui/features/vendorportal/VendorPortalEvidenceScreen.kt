package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.RegisterCollaborationEvidenceRequestDto
import com.sucharu.sucharupro.data.api.model.VendorCollaborationEvidenceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalEvidenceScreen(
    resourceType: String,
    resourceId: String,
    evidenceList: List<VendorCollaborationEvidenceDto>,
    onRegisterEvidence: (RegisterCollaborationEvidenceRequestDto) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showRegisterDialog by remember { mutableStateOf(false) }
    var filenameText by remember { mutableStateOf("") }
    var fileReferenceText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Collaboration Evidence & Attachments",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRegisterDialog = true },
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White
            ) {
                Text("+ Attach Evidence", modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                if (evidenceList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No evidence documents registered yet.", color = Color(0xFF64748B))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(evidenceList) { ev ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ev.filename, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("${ev.mimeType} • ${ev.sizeBytes} bytes", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        ev.description?.let {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(it, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                        }
                                    }
                                    Surface(
                                        color = if (ev.visibility == "VENDOR_VISIBLE") Color(0xFF065F46) else Color(0xFF334155),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = ev.visibility,
                                            color = if (ev.visibility == "VENDOR_VISIBLE") Color(0xFF34D399) else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Register Evidence File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = filenameText,
                        onValueChange = { filenameText = it },
                        label = { Text("Filename (e.g. inspection_photo.jpg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fileReferenceText,
                        onValueChange = { fileReferenceText = it },
                        label = { Text("Storage URI / Ref") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descriptionText,
                        onValueChange = { descriptionText = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRegisterEvidence(
                            RegisterCollaborationEvidenceRequestDto(
                                resourceType = resourceType,
                                resourceId = resourceId,
                                fileReference = fileReferenceText.ifBlank { "gcs://evidence/${filenameText}" },
                                filename = filenameText,
                                mimeType = "image/jpeg",
                                sizeBytes = 1024L,
                                description = descriptionText.ifBlank { null }
                            )
                        )
                        showRegisterDialog = false
                    }
                ) {
                    Text("Register")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) { Text("Cancel") }
            }
        )
    }
}
