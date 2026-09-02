package com.sucharu.sucharupro.ui.features.vendorportal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.sucharu.sucharupro.data.api.model.VendorPortalQualityWorkspaceDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalQualityWorkspaceScreen(
    workspace: VendorPortalQualityWorkspaceDto,
    onViewQualityCasesClick: () -> Unit = {},
    onViewInspectionsClick: () -> Unit = {},
    onViewRejectionsClick: () -> Unit = {},
    onViewCapaPlansClick: () -> Unit = {},
    onViewDisputesClick: () -> Unit = {},
    onRaiseDisputeClick: () -> Unit = {},
    onCreateCapaClick: () -> Unit = {},
    onCaseClick: (String) -> Unit = {},
    onCapaClick: (String) -> Unit = {},
    onDisputeClick: (String) -> Unit = {},
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
                        text = "Quality, CAPA & Dispute Workspace",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    Button(
                        onClick = onRaiseDisputeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Raise Dispute", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onCreateCapaClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("+ Draft CAPA", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Section
            item {
                Text(
                    text = "Quality Metrics & Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Pass Rate",
                        value = "${workspace.kpiSummary.qualityPassRate}%",
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Open Cases",
                        value = "${workspace.kpiSummary.openQualityCases}",
                        color = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Active CAPAs",
                        value = "${workspace.kpiSummary.activeCapaCount}",
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Open Disputes",
                        value = "${workspace.kpiSummary.openDisputesCount}",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Navigation Hub
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewQualityCasesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quality Cases", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onViewInspectionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Inspections", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onViewRejectionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rejections", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onViewCapaPlansClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CAPA Plans", color = Color.White, fontSize = 12.sp)
                    }
                    Button(
                        onClick = onViewDisputesClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Disputes", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            // Recent Quality Cases
            item {
                Text(
                    text = "Recent Quality Cases",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            if (workspace.recentCases.isEmpty()) {
                item {
                    Text("No quality cases found.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                items(workspace.recentCases) { case ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCaseClick(case.caseId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = case.caseNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = case.status,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = case.title, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                        }
                    }
                }
            }

            // Active CAPAs
            item {
                Text(
                    text = "Active Corrective & Preventive Action (CAPA) Plans",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            if (workspace.activeCapas.isEmpty()) {
                item {
                    Text("No active CAPA plans.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                items(workspace.activeCapas) { capa ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCapaClick(capa.capaId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = capa.capaNumber,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = capa.status,
                                    color = Color(0xFFF59E0B),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = capa.title, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                            Text(
                                text = "Responsible: ${capa.responsiblePerson}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Active Disputes
            item {
                Text(
                    text = "Active Disputes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )
            }

            if (workspace.activeDisputes.isEmpty()) {
                item {
                    Text("No active disputes.", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                items(workspace.activeDisputes) { disp ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDisputeClick(disp.disputeId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = disp.disputeReference,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = disp.status,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = disp.subject, color = Color(0xFFE2E8F0), fontSize = 14.sp)
                            Text(
                                text = "Type: ${disp.disputeType} • Resolution: ${disp.requestedResolution}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
