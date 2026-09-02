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
import com.sucharu.sucharupro.data.api.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorPortalPerformanceWorkspaceScreen(
    workspace: VendorPortalPerformanceWorkspaceDto,
    onViewScorecardsClick: () -> Unit = {},
    onViewEvaluationsClick: () -> Unit = {},
    onViewKpisClick: () -> Unit = {},
    onViewTrendsClick: () -> Unit = {},
    onViewComplianceClick: () -> Unit = {},
    onViewCertificationsClick: () -> Unit = {},
    onViewExpiriesClick: () -> Unit = {},
    onViewEvidenceClick: () -> Unit = {},
    onViewCorrectiveActionsClick: () -> Unit = {},
    onScorecardClick: (String) -> Unit = {},
    onEvaluationClick: (String) -> Unit = {},
    onCorrectiveActionClick: (String) -> Unit = {},
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
                        text = "Vendor Performance & Compliance",
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Overview Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PerformanceSummaryMetricCard(
                        title = "Overall Score",
                        value = "${String.format("%.1f", workspace.overview.overallScore)}%",
                        subtitle = "Rating: ${workspace.overview.rating}",
                        indicatorColor = if (workspace.overview.overallScore >= 80.0) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceSummaryMetricCard(
                        title = "Compliance Rate",
                        value = "${String.format("%.1f", workspace.complianceOverview.complianceRate)}%",
                        subtitle = "Risk: ${workspace.complianceOverview.overallRiskLevel}",
                        indicatorColor = if (workspace.complianceOverview.overallRiskLevel == "LOW") Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceSummaryMetricCard(
                        title = "Open CAPAs",
                        value = "${workspace.overview.openCorrectiveActions}",
                        subtitle = "Action items",
                        indicatorColor = if (workspace.overview.openCorrectiveActions == 0) Color(0xFF10B981) else Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                    PerformanceSummaryMetricCard(
                        title = "Urgent Expiries",
                        value = "${workspace.complianceOverview.upcomingExpiringCertificationsCount + workspace.complianceOverview.expiredCertificationsCount}",
                        subtitle = "Within 30 days",
                        indicatorColor = if (workspace.complianceOverview.expiredCertificationsCount > 0) Color(0xFFEF4444) else Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Navigation Action Pills
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onViewScorecardsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Scorecards", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onViewEvaluationsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Evaluations", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onViewKpisClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("KPIs", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onViewTrendsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Trends", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onViewComplianceClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Compliance", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                    Button(
                        onClick = onViewCorrectiveActionsClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("CAPA Actions", color = Color(0xFF38BDF8), fontSize = 13.sp)
                    }
                }
            }

            // Recent Scorecards Section
            item {
                Text(
                    text = "Recent Scorecards",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (workspace.recentScorecards.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "No scorecards generated yet.")
                }
            } else {
                items(workspace.recentScorecards) { sc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onScorecardClick(sc.scorecardId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Scorecard #${sc.scorecardId.take(10)} • ${sc.periodType}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Rating: ${sc.rating} • Risk: ${sc.riskLevel}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${String.format("%.1f", sc.overallScore)}%",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = sc.status,
                                    color = if (sc.status == "APPROVED") Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Pending Evaluations Section
            item {
                Text(
                    text = "Pending Evaluations & Feedback",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (workspace.pendingEvaluations.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "No pending evaluations requiring review.")
                }
            } else {
                items(workspace.pendingEvaluations) { ev ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvaluationClick(ev.evaluationId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Evaluation #${ev.evaluationId.take(10)} • Score: ${String.format("%.1f", ev.evaluationScore)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Decision: ${ev.decision ?: "PENDING"} • Status: ${ev.status}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = { onEvaluationClick(ev.evaluationId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Respond", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Urgent Expiries Section
            item {
                Text(
                    text = "Certification & Compliance Alerts",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (workspace.urgentExpiries.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "All certifications and compliance documents are up to date.")
                }
            } else {
                items(workspace.urgentExpiries) { exp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (exp.alertLevel == "EXPIRED") Color(0x33EF4444) else Color(0x22F59E0B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = exp.certificationName,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Code: ${exp.requirementCode} • ${if (exp.daysRemaining < 0) "Expired ${-exp.daysRemaining} days ago" else "${exp.daysRemaining} days remaining"}",
                                    color = if (exp.alertLevel == "EXPIRED") Color(0xFFFCA5A5) else Color(0xFFFDE68A),
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = exp.alertLevel,
                                color = if (exp.alertLevel == "EXPIRED") Color(0xFFEF4444) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Open Corrective Actions Section
            item {
                Text(
                    text = "Active Corrective Actions (CAPA)",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (workspace.openCorrectiveActions.isEmpty()) {
                item {
                    EmptySectionPlaceholder(message = "No open corrective actions assigned.")
                }
            } else {
                items(workspace.openCorrectiveActions) { ca ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCorrectiveActionClick(ca.actionId) },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "CAPA #${ca.actionId.take(10)} • Priority: ${ca.priority}",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = ca.status,
                                    color = if (ca.isOverdue) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = ca.issueDescription,
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                            if (ca.isOverdue) {
                                Text(
                                    text = "⚠ OVERDUE",
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PerformanceSummaryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = indicatorColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun EmptySectionPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = Color(0xFF64748B), fontSize = 14.sp)
    }
}
