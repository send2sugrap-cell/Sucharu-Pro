package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalComplianceEvidenceDto
import com.sucharu.sucharupro.data.api.model.VendorPortalComplianceEvidenceUploadRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalComplianceEvidenceScreen(
    evidenceList: List<VendorPortalComplianceEvidenceDto>,
    recordId: String? = null,
    actionId: String? = null,
    onUploadEvidence: (VendorPortalComplianceEvidenceUploadRequest) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var fileName by remember { mutableStateOf("") }
    var fileUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Compliance Evidence & Documents",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Back", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF020617)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Upload New Evidence",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("File Name (e.g. ISO9001_Cert.pdf)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        OutlinedTextField(
                            value = fileUrl,
                            onValueChange = { fileUrl = it },
                            label = { Text("File URL / Storage URI", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description & Notes (Optional)", color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF38BDF8),
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )

                        Button(
                            onClick = {
                                if (fileName.isNotBlank() && fileUrl.isNotBlank()) {
                                    onUploadEvidence(
                                        VendorPortalComplianceEvidenceUploadRequest(
                                            recordId = recordId,
                                            actionId = actionId,
                                            fileName = fileName,
                                            fileUrl = fileUrl,
                                            description = description.ifBlank { null }
                                        )
                                    )
                                    fileName = ""
                                    fileUrl = ""
                                    description = ""
                                }
                            },
                            enabled = fileName.isNotBlank() && fileUrl.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Upload Evidence Document", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Uploaded Evidence (${evidenceList.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (evidenceList.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "No evidence documents uploaded yet.")
                }
            } else {
                items(evidenceList) { ev ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = ev.fileName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Type: ${ev.evidenceType} • Uploaded by: ${ev.uploadedBy}",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            val desc = ev.description
                            if (!desc.isNullOrBlank()) {
                                Text(
                                    text = desc,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
