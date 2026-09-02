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
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityEvidenceDto
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityEvidenceUploadRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityEvidenceScreen(
    entityType: String,
    entityId: String,
    evidenceList: List<VendorPortalQualityEvidenceDto>,
    onUploadEvidence: (VendorPortalQualityEvidenceUploadRequest) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    var filename by remember { mutableStateOf("") }
    var fileReference by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var evidenceType by remember { mutableStateOf("DOCUMENT") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quality Evidence ($entityType)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Upload New Evidence", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

                    OutlinedTextField(
                        value = filename,
                        onValueChange = { filename = it },
                        label = { Text("Filename (e.g. lab_report.pdf)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = fileReference,
                        onValueChange = { fileReference = it },
                        label = { Text("File Reference / URI") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onUploadEvidence(
                                VendorPortalQualityEvidenceUploadRequest(
                                    entityType = entityType,
                                    entityId = entityId,
                                    evidenceType = evidenceType,
                                    filename = filename,
                                    fileReference = fileReference,
                                    description = description
                                )
                            )
                            filename = ""
                            fileReference = ""
                            description = ""
                        },
                        enabled = filename.isNotBlank() && fileReference.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload Evidence Document", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Attached Evidence Files", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), fontSize = 15.sp)

            if (evidenceList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No evidence documents attached.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(evidenceList) { ev ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = ev.filename,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = ev.evidenceType,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp
                                    )
                                }
                                ev.description?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = it, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ref: ${ev.fileReference} • Uploaded by ${ev.uploadedBy}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
