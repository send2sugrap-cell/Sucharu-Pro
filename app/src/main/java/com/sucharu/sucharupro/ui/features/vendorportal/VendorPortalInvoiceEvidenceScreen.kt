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
import com.sucharu.sucharupro.data.api.model.UploadFinancialEvidenceRequestDto
import com.sucharu.sucharupro.data.api.model.VendorPortalFinancialEvidenceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalInvoiceEvidenceScreen(
    evidenceList: List<VendorPortalFinancialEvidenceDto>,
    entityId: String,
    onUploadEvidenceClick: (UploadFinancialEvidenceRequestDto) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    var showUploadModal by remember { mutableStateOf(false) }
    var filename by remember { mutableStateOf("") }
    var fileReference by remember { mutableStateOf("") }
    var evidenceType by remember { mutableStateOf("INVOICE_DOCUMENT") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Financial Evidence & Documents",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = { showUploadModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("+ Upload Document", color = Color.White, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (evidenceList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No financial documents attached.", color = Color.Gray, fontSize = 15.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(evidenceList) { ev ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(ev.filename, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Type: ${ev.evidenceType} • Size: ${ev.sizeBytes / 1024} KB", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Ref: ${ev.fileReference}", fontSize = 11.sp, color = Color(0xFF38BDF8))
                            }
                        }
                    }
                }
            }
        }
    }
}
