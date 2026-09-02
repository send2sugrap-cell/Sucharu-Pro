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
import com.sucharu.sucharupro.data.api.model.ReportBlockerRequestDto
import com.sucharu.sucharupro.data.api.model.VendorBlockerDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalBlockerScreen(
    workOrderId: String?,
    blockers: List<VendorBlockerDto>,
    onReportBlocker: (ReportBlockerRequestDto) -> Unit = {},
    onAcknowledgeBlocker: (String) -> Unit = {},
    onResolveBlocker: (String, String) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var blockerTitle by remember { mutableStateOf("") }
    var blockerDescription by remember { mutableStateOf("") }
    var blockerCategory by remember { mutableStateOf("MATERIAL_SHORTAGE") }
    var blockerSeverity by remember { mutableStateOf("HIGH") }

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Vendor Blockers & Issues",
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
            if (workOrderId != null) {
                FloatingActionButton(
                    onClick = { showReportDialog = true },
                    containerColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ) {
                    Text("⚠ Report Blocker", modifier = Modifier.padding(horizontal = 16.dp))
                }
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
                if (blockers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No blockers reported. Everything running smoothly!", color = Color(0xFF34D399))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(blockers) { blocker ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (blocker.status == "OPEN") Color(0xFF291B1B) else Color(0xFF1E293B)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = blocker.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        BlockerStatusBadge(status = blocker.status, severity = blocker.severity)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = blocker.description,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Category: ${blocker.category}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                                        Text("Severity: ${blocker.severity}", fontSize = 12.sp, color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                                    }
                                    blocker.resolutionNotes?.let { res ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Resolution: $res", fontSize = 12.sp, color = Color(0xFF34D399))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report Operational Blocker") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = blockerTitle,
                        onValueChange = { blockerTitle = it },
                        label = { Text("Issue Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = blockerDescription,
                        onValueChange = { blockerDescription = it },
                        label = { Text("Detailed Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (workOrderId != null) {
                            onReportBlocker(
                                ReportBlockerRequestDto(
                                    workOrderId = workOrderId,
                                    category = blockerCategory,
                                    severity = blockerSeverity,
                                    title = blockerTitle,
                                    description = blockerDescription
                                )
                            )
                        }
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Submit Blocker")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun BlockerStatusBadge(status: String, severity: String) {
    val (bgColor, textColor) = when (status) {
        "OPEN" -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        "ACKNOWLEDGED" -> Color(0xFF78350F) to Color(0xFFFBBF24)
        "RESOLVED" -> Color(0xFF065F46) to Color(0xFF34D399)
        else -> Color(0xFF1E293B) to Color(0xFF94A3B8)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
