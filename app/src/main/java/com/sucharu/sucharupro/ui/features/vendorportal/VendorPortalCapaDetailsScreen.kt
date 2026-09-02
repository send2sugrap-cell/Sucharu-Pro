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
import com.sucharu.sucharupro.data.api.model.VendorPortalCapaPlanDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalCapaDetailsScreen(
    capa: VendorPortalCapaPlanDto,
    onSubmitCapaClick: () -> Unit = {},
    onCompleteCapaClick: () -> Unit = {},
    onAddActionClick: () -> Unit = {},
    onCompleteActionClick: (String) -> Unit = {},
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
                        text = "CAPA Plan: ${capa.capaNumber}",
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
            // Overview Card
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
                            text = capa.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = capa.status,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Root Cause Analysis:", fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(capa.rootCause, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Corrective Action:", fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(capa.correctiveAction, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Preventive Action:", fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8), fontSize = 13.sp)
                    Text(capa.preventiveAction, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0xFF334155))
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Owner", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(capa.responsiblePerson, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Priority", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(capa.priority, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("Target Date", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                java.time.Instant.ofEpochMilli(capa.targetCompletionDate).toString().substringBefore("T"),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Action Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Action Items (${capa.actions.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                        TextButton(onClick = onAddActionClick) {
                            Text("+ Add Action", color = Color(0xFF38BDF8))
                        }
                    }

                    if (capa.actions.isEmpty()) {
                        Text("No specific action items defined yet.", color = Color(0xFF64748B), fontSize = 13.sp)
                    } else {
                        capa.actions.forEach { action ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "#${action.actionNumber} [${action.actionType}] ${action.description}",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Owner: ${action.owner} • Status: ${action.status}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (action.status != "COMPLETED") {
                                        Button(
                                            onClick = { onCompleteActionClick(action.actionId) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Done", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Lifecycle Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (capa.status == "DRAFT") {
                    Button(
                        onClick = onSubmitCapaClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Submit CAPA for Approval", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (capa.status in setOf("APPROVED", "IN_PROGRESS")) {
                    Button(
                        onClick = onCompleteCapaClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Mark CAPA as Completed", color = Color.White, fontWeight = FontWeight.Bold)
                    }
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
