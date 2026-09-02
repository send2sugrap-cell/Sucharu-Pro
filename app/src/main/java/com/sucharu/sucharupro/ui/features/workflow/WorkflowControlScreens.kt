package com.sucharu.sucharupro.ui.features.workflow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.domain.workflow.governance.*
import com.sucharu.sucharupro.domain.workflow.model.ApprovalStatus
import com.sucharu.sucharupro.domain.workflow.model.WorkflowStatus

/**
 * Production-Grade Dark Navy Workflow Control Plane & Operations Console UI (INFRA-04 Step 06).
 */
@Composable
fun WorkflowDashboardScreen(
    principal: AuthenticatedPrincipal,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Monitor", "Approvals", "Definitions", "Metrics", "Audit Trail")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B132B))
            .padding(16.dp)
    ) {
        // Control Plane Header
        Surface(
            color = Color(0xFF1C2541),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "WORKFLOW CONTROL PLANE",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9ECAFF),
                        fontSize = 16.sp
                    )
                    Text(
                        "Commercial Printing Process Orchestration & Governance",
                        color = Color(0xFFB7C8D8),
                        fontSize = 11.sp
                    )
                }
                Surface(
                    color = Color(0xFF00497D),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Tenant: ${principal.projectId}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1C2541),
            contentColor = Color(0xFF9ECAFF)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (selectedTab) {
                0 -> WorkflowInstanceMonitorView(principal)
                1 -> WorkflowApprovalQueueView(principal)
                2 -> WorkflowDefinitionsView(principal)
                3 -> WorkflowMetricsView(principal)
                4 -> WorkflowAuditTrailView(principal)
            }
        }
    }
}

@Composable
fun WorkflowStatusBadge(status: WorkflowStatus) {
    val (bgColor, textColor) = when (status) {
        WorkflowStatus.RUNNING -> Color(0xFF00497D) to Color(0xFF9ECAFF)
        WorkflowStatus.WAITING, WorkflowStatus.WAITING_APPROVAL -> Color(0xFF5C4300) to Color(0xFFFFD700)
        WorkflowStatus.COMPLETED -> Color(0xFF005A36) to Color(0xFF85DF9E)
        WorkflowStatus.FAILED, WorkflowStatus.DEAD_LETTER -> Color(0xFF8C1D40) to Color(0xFFFFB4AB)
        WorkflowStatus.COMPENSATING -> Color(0xFF6B3A00) to Color(0xFFFFB77F)
        WorkflowStatus.PAUSED -> Color(0xFF4A148C) to Color(0xFFE1BEE7)
        WorkflowStatus.CANCELLED -> Color(0xFF37474F) to Color(0xFFB0BEC5)
        else -> Color(0xFF1C2541) to Color.White
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = status.name,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WorkflowInstanceMonitorView(principal: AuthenticatedPrincipal) {
    val sampleInstances = remember {
        listOf(
            WorkflowInstanceSummary(
                workflowId = "wf-inst-901",
                projectId = principal.projectId,
                definitionId = "commercial-brochure-print",
                definitionName = "Commercial Brochure Print",
                versionId = "v1",
                versionNumber = 1,
                status = WorkflowStatus.RUNNING,
                currentStepId = "laser-plate-exposure",
                currentStepName = "Laser Plate Exposure",
                progressPercent = 50,
                actorType = principal.principalType,
                actorId = principal.userId,
                startedAt = System.currentTimeMillis() - 120000,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            ),
            WorkflowInstanceSummary(
                workflowId = "wf-inst-902",
                projectId = principal.projectId,
                definitionId = "high-volume-magazine-run",
                definitionName = "High Volume Magazine Run",
                versionId = "v1",
                versionNumber = 1,
                status = WorkflowStatus.WAITING_APPROVAL,
                currentStepId = "manager-discount-approval",
                currentStepName = "Manager Discount Approval",
                progressPercent = 60,
                actorType = principal.principalType,
                actorId = principal.userId,
                startedAt = System.currentTimeMillis() - 360000,
                completedAt = null,
                updatedAt = System.currentTimeMillis()
            ),
            WorkflowInstanceSummary(
                workflowId = "wf-inst-903",
                projectId = principal.projectId,
                definitionId = "custom-packaging-box",
                definitionName = "Custom Packaging Box",
                versionId = "v2",
                versionNumber = 2,
                status = WorkflowStatus.COMPLETED,
                currentStepId = "dispatch-notification",
                currentStepName = "Dispatch Notification",
                progressPercent = 100,
                actorType = principal.principalType,
                actorId = principal.userId,
                startedAt = System.currentTimeMillis() - 7200000,
                completedAt = System.currentTimeMillis() - 180000,
                updatedAt = System.currentTimeMillis() - 180000
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sampleInstances) { inst ->
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(inst.definitionName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        WorkflowStatusBadge(inst.status)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Instance: ${inst.workflowId} | Step: ${inst.currentStepName ?: "None"}", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { inst.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color(0xFF9ECAFF),
                        trackColor = Color(0xFF0B132B)
                    )
                }
            }
        }
    }
}

@Composable
fun WorkflowApprovalQueueView(principal: AuthenticatedPrincipal) {
    val sampleApprovals = remember {
        listOf(
            WorkflowApprovalSummary(
                approvalId = "appr-req-101",
                workflowId = "wf-inst-902",
                stepId = "manager-discount-approval",
                policyId = "pol-discount-over-10k",
                policyName = "Discounts Exceeding $10,000",
                requiredRole = UserRole.MANAGER,
                status = ApprovalStatus.PENDING,
                requesterId = "usr_staff_12",
                requesterRole = UserRole.STAFF,
                approvalsReceived = 0,
                approvalsRequired = 1,
                allowSelfApproval = false,
                isEscalated = false,
                timeoutAt = System.currentTimeMillis() + 86400000,
                createdAt = System.currentTimeMillis() - 360000
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sampleApprovals) { appr ->
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(appr.policyName, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), fontSize = 13.sp)
                        Surface(color = Color(0xFF5C4300), shape = RoundedCornerShape(8.dp)) {
                            Text("Required: ${appr.requiredRole.name}", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Requester: ${appr.requesterId} (${appr.requesterRole.name})", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                    Text("Workflow: ${appr.workflowId} | Step: ${appr.stepId}", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005A36)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Approve", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {},
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reject", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkflowDefinitionsView(principal: AuthenticatedPrincipal) {
    val sampleDefinitions = remember {
        listOf(
            WorkflowDefinitionSummary(
                definitionId = "commercial-brochure-print",
                projectId = principal.projectId,
                name = "Commercial Brochure Print",
                description = "Automated preflight, RIP, plate imaging, and dispatch workflow",
                category = "PRODUCTION",
                isEnabled = true,
                latestVersion = 1,
                activeVersionId = "v1",
                totalInstances = 142,
                createdAt = System.currentTimeMillis() - 864000000,
                updatedAt = System.currentTimeMillis() - 86400000
            ),
            WorkflowDefinitionSummary(
                definitionId = "high-volume-magazine-run",
                projectId = principal.projectId,
                name = "High Volume Magazine Run",
                description = "Multi-step web offset printing with automated quality gate",
                category = "OFFSET",
                isEnabled = true,
                latestVersion = 2,
                activeVersionId = "v2",
                totalInstances = 89,
                createdAt = System.currentTimeMillis() - 604800000,
                updatedAt = System.currentTimeMillis() - 3600000
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sampleDefinitions) { def ->
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(def.name, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 14.sp)
                        Surface(color = Color(0xFF00497D), shape = RoundedCornerShape(6.dp)) {
                            Text("Active: ${def.activeVersionId ?: "None"}", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(def.description ?: "", color = Color(0xFFB7C8D8), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun WorkflowMetricsView(principal: AuthenticatedPrincipal) {
    Surface(
        color = Color(0xFF1C2541),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("TENANT WORKFLOW METRICS", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Active Running Workflows", color = Color.White, fontSize = 12.sp)
                Text("12", fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pending Human Approvals", color = Color.White, fontSize = 12.sp)
                Text("3", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Failed / Dead-Letter Workflows", color = Color.White, fontSize = 12.sp)
                Text("0", fontWeight = FontWeight.Bold, color = Color(0xFF85DF9E), fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Average Step Latency", color = Color.White, fontSize = 12.sp)
                Text("380 ms", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun WorkflowAuditTrailView(principal: AuthenticatedPrincipal) {
    val sampleAudit = remember {
        listOf(
            WorkflowAuditEntry(
                auditId = "wf-aud-01",
                projectId = principal.projectId,
                actorId = principal.userId,
                actorRole = principal.role,
                principalType = principal.principalType,
                operation = "PUBLISH_WORKFLOW_VERSION",
                targetType = "WorkflowVersion",
                targetId = "commercial-brochure-print:v1",
                previousState = "DRAFT",
                newState = "PUBLISHED",
                details = "Published immutable version v1",
                clientIp = "127.0.0.1",
                correlationId = "corr-pub-991"
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(sampleAudit) { aud ->
            Surface(
                color = Color(0xFF1C2541),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(aud.operation, fontWeight = FontWeight.Bold, color = Color(0xFF9ECAFF), fontSize = 12.sp)
                    Text("Target: ${aud.targetType} [${aud.targetId}]", color = Color.White, fontSize = 11.sp)
                    Text("Actor: ${aud.actorId} (${aud.actorRole.name}) | ${aud.details ?: ""}", color = Color(0xFFB7C8D8), fontSize = 10.sp)
                }
            }
        }
    }
}
