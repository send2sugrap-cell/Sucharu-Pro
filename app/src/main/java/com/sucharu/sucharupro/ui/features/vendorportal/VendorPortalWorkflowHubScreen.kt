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
import com.sucharu.sucharupro.data.api.model.VendorWorkflowDto
import com.sucharu.sucharupro.data.api.model.VendorWorkflowHubSummaryDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalWorkflowHubScreen(
    summary: VendorWorkflowHubSummaryDto,
    onWorkflowClick: (String) -> Unit = {},
    onViewExceptionsClick: () -> Unit = {},
    onViewNextActionsClick: () -> Unit = {},
    onSyncWorkflowClick: (String) -> Unit = {},
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
                        text = "End-to-End Workflow Command Center",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF020617)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgGradient)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Stat Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkflowStatCard(
                        title = "Active Workflows",
                        value = summary.totalActiveWorkflows.toString(),
                        accentColor = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    WorkflowStatCard(
                        title = "Blocked / Exception",
                        value = summary.blockedWorkflows.toString(),
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WorkflowStatCard(
                        title = "Overdue SLAs",
                        value = summary.overdueWorkflows.toString(),
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    WorkflowStatCard(
                        title = "Avg Cycle Time",
                        value = "${summary.averageCycleTimeDays}d",
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Action Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onViewNextActionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pending Actions (${summary.urgentActions.size})", color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onViewExceptionsClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Exceptions (${summary.blockedWorkflows})")
                    }
                }
            }

            // Stage Distribution Breakdown
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Stage Distribution",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (summary.stageBreakdown.isEmpty()) {
                            Text("No active stages", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            summary.stageBreakdown.forEach { (stage, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stage.replace("_", " "), color = Color(0xFFCBD5E1), fontSize = 14.sp)
                                    Text("$count", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Active Workflows Header
            item {
                Text(
                    text = "Active Commercial & Production Workflows",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (summary.recentWorkflows.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No active commercial workflows found.", color = Color.Gray)
                        }
                    }
                }
            } else {
                items(summary.recentWorkflows) { wf ->
                    WorkflowListItemCard(
                        workflow = wf,
                        onClick = { onWorkflowClick(wf.workflowId) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 13.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun WorkflowListItemCard(
    workflow: VendorWorkflowDto,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workflow.workflowTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                val statusColor = when (workflow.status) {
                    "ACTIVE" -> Color(0xFF38BDF8)
                    "COMPLETED" -> Color(0xFF10B981)
                    "BLOCKED", "EXCEPTION" -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = workflow.status,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Stage: ${workflow.currentStage.replace("_", " ")}",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = "SLA: ${workflow.slaStatus}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (workflow.slaStatus == "OVERDUE") Color(0xFFEF4444) else Color(0xFF10B981)
                )
            }
        }
    }
}
