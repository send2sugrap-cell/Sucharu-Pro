package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityCaseDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityCaseDetailsScreen(
    case: VendorPortalQualityCaseDto,
    onAcknowledgeClick: () -> Unit = {},
    onRespondClick: () -> Unit = {},
    onCreateCapaClick: () -> Unit = {},
    onViewInspectionClick: (String) -> Unit = {},
    onViewRejectionClick: (String) -> Unit = {},
    onViewEvidenceClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quality Case: ${case.caseNumber}",
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = case.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = case.status,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = case.description,
                        color = Color(0xFFE2E8F0),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Severity", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(case.severity, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Acknowledged", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                if (case.acknowledgedAt != null) "Yes" else "No",
                                color = if (case.acknowledgedAt != null) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Created", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                java.time.Instant.ofEpochMilli(case.createdAt).toString().substringBefore("T"),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Linked Canonical References
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Linked Records", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

                    case.inspectionId?.let { inspId ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inspection ID: $inspId", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            TextButton(onClick = { onViewInspectionClick(inspId) }) {
                                Text("View Inspection", color = Color(0xFF38BDF8), fontSize = 13.sp)
                            }
                        }
                    }

                    case.rejectionId?.let { rejId ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Rejection ID: $rejId", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            TextButton(onClick = { onViewRejectionClick(rejId) }) {
                                Text("View Rejection", color = Color(0xFF38BDF8), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (case.status == "OPEN") {
                    Button(
                        onClick = onAcknowledgeClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acknowledge Quality Case", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onRespondClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Submit Response / Action Plan", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onCreateCapaClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create Formal CAPA Plan", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onViewEvidenceClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("View & Upload Evidence", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}
