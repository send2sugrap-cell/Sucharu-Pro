package com.sucharu.sucharupro.ui.features.adjustment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.businessfinancialadjustment.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AdjustmentTab(val title: String, val icon: ImageVector) {
    ADJUSTMENTS("Adjustments", Icons.Default.Tune),
    REFUNDS("Refunds", Icons.Default.CurrencyExchange),
    WRITE_OFFS("Write-Offs", Icons.Default.MoneyOff),
    REVERSALS("Reversals", Icons.Default.Undo),
    EXCEPTIONS("Exceptions", Icons.Default.WarningAmber),
    AUDIT("Audit Trail", Icons.Default.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessFinancialAdjustmentManagementScreen(
    summary: FinancialAdjustmentSummaryResponse? = null,
    adjustments: List<FinancialAdjustmentResponse> = emptyList(),
    refunds: List<RefundResponse> = emptyList(),
    writeOffs: List<WriteOffResponse> = emptyList(),
    exceptions: List<FinancialExceptionResponse> = emptyList(),
    auditEvents: List<FinancialAdjustmentAuditEventResponse> = emptyList(),
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRefresh: () -> Unit = {},
    onCreateAdjustment: (CreateAdjustmentRequest) -> Unit = {},
    onSubmitAdjustment: (String) -> Unit = {},
    onApproveAdjustment: (String, String?) -> Unit = { _, _ -> },
    onRejectAdjustment: (String, String) -> Unit = { _, _ -> },
    onCancelAdjustment: (String, String) -> Unit = { _, _ -> },
    onPostAdjustment: (String) -> Unit = {},
    onReverseAdjustment: (String, String) -> Unit = { _, _ -> },
    onCreateRefund: (CreateRefundRequest) -> Unit = {},
    onApproveRefund: (String, String?) -> Unit = { _, _ -> },
    onPostRefund: (String) -> Unit = {},
    onCreateWriteOff: (CreateWriteOffRequest) -> Unit = {},
    onApproveWriteOff: (String, String?) -> Unit = { _, _ -> },
    onPostWriteOff: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(AdjustmentTab.ADJUSTMENTS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FINANCIAL ADJUSTMENTS & CORRECTIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF00E5FF)
                        )
                        Text(
                            text = "Controlled Compensating Postings, Refunds, Write-Offs & Exception Governance",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00E5FF))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D1B2A)
                )
            )
        },
        containerColor = Color(0xFF0A0E17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF721C24)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Top Summary KPI Cards
            if (summary != null) {
                AdjustmentMetricsHeader(summary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Navigation Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF111827),
                contentColor = Color(0xFF00E5FF),
                edgePadding = 0.dp,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                AdjustmentTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00E5FF))
                }
            } else {
                when (selectedTab) {
                    AdjustmentTab.ADJUSTMENTS -> AdjustmentsListView(
                        adjustments = adjustments,
                        onSubmit = onSubmitAdjustment,
                        onApprove = onApproveAdjustment,
                        onReject = onRejectAdjustment,
                        onCancel = onCancelAdjustment,
                        onPost = onPostAdjustment,
                        onReverse = onReverseAdjustment
                    )
                    AdjustmentTab.REFUNDS -> RefundsListView(
                        refunds = refunds,
                        onApprove = onApproveRefund,
                        onPost = onPostRefund
                    )
                    AdjustmentTab.WRITE_OFFS -> WriteOffsListView(
                        writeOffs = writeOffs,
                        onApprove = onApproveWriteOff,
                        onPost = onPostWriteOff
                    )
                    AdjustmentTab.REVERSALS -> ReversalsListView(
                        adjustments = adjustments.filter { it.status in setOf("POSTED", "REVERSAL_REQUESTED", "REVERSAL_APPROVED", "REVERSED") },
                        onReverse = onReverseAdjustment
                    )
                    AdjustmentTab.EXCEPTIONS -> ExceptionsListView(exceptions = exceptions)
                    AdjustmentTab.AUDIT -> AdjustmentAuditListView(auditEvents = auditEvents)
                }
            }
        }
    }
}

@Composable
fun AdjustmentMetricsHeader(summary: FinancialAdjustmentSummaryResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricCard("Total Adjusted", "BDT ${summary.totalAdjustedAmount}", Color(0xFF00E5FF), Modifier.weight(1f))
        MetricCard("Total Refunded", "BDT ${summary.totalRefundedAmount}", Color(0xFF00E676), Modifier.weight(1f))
        MetricCard("Total Write-Off", "BDT ${summary.totalWrittenOffAmount}", Color(0xFFFF5252), Modifier.weight(1f))
        MetricCard("Pending Approvals", "${summary.pendingAdjustmentsCount}", Color(0xFFFFAB00), Modifier.weight(1f))
    }
}

@Composable
fun MetricCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16202E)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
fun AdjustmentsListView(
    adjustments: List<FinancialAdjustmentResponse>,
    onSubmit: (String) -> Unit,
    onApprove: (String, String?) -> Unit,
    onReject: (String, String) -> Unit,
    onCancel: (String, String) -> Unit,
    onPost: (String) -> Unit,
    onReverse: (String, String) -> Unit
) {
    if (adjustments.isEmpty()) {
        EmptyStateCard("No financial adjustments recorded yet.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(adjustments) { adj ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(adj.adjustmentNumber, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 15.sp)
                            Text("${adj.adjustmentType} • ${adj.sourceType}", fontSize = 12.sp, color = Color.LightGray)
                        }
                        StatusBadge(adj.status)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reason: ${adj.reason}", fontSize = 13.sp, color = Color.White)
                    Text("Justification: ${adj.justification}", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Original: BDT ${adj.originalAmount}", fontSize = 12.sp, color = Color.Gray)
                        Text("Adjustment: BDT ${adj.adjustmentAmount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD54F))
                        Text("Effective: BDT ${adj.effectiveAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    }

                    if (adj.ledgerPostingId != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ledger Posting: #${adj.ledgerPostingId}", fontSize = 11.sp, color = Color(0xFF81D4FA))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (adj.status == "DRAFT") {
                            Button(
                                onClick = { onSubmit(adj.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Submit", fontSize = 11.sp)
                            }
                        }
                        if (adj.status in setOf("SUBMITTED", "UNDER_REVIEW")) {
                            Button(
                                onClick = { onApprove(adj.id, null) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Approve", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onReject(adj.id, "Rejected by manager") },
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Reject", fontSize = 11.sp, color = Color(0xFFFF5252))
                            }
                        }
                        if (adj.status == "APPROVED") {
                            Button(
                                onClick = { onPost(adj.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Post to Ledger", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RefundsListView(
    refunds: List<RefundResponse>,
    onApprove: (String, String?) -> Unit,
    onPost: (String) -> Unit
) {
    if (refunds.isEmpty()) {
        EmptyStateCard("No refund requests found.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(refunds) { ref ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ref.refundNumber, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), fontSize = 15.sp)
                        StatusBadge(ref.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Reason: ${ref.refundReason}", fontSize = 13.sp, color = Color.White)
                    Text("Method: ${ref.paymentMethod} • Period: ${ref.periodId}", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Eligible: BDT ${ref.eligibleBalance}", fontSize = 12.sp, color = Color.Gray)
                        Text("Requested: BDT ${ref.requestedAmount}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFD54F))
                        Text("Approved: BDT ${ref.approvedAmount}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                    }

                    if (ref.status == "REQUESTED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onApprove(ref.id, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Approve Refund", fontSize = 11.sp)
                        }
                    } else if (ref.status == "APPROVED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onPost(ref.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Post Refund", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WriteOffsListView(
    writeOffs: List<WriteOffResponse>,
    onApprove: (String, String?) -> Unit,
    onPost: (String) -> Unit
) {
    if (writeOffs.isEmpty()) {
        EmptyStateCard("No write-off records found.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(writeOffs) { wo ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(wo.writeOffNumber, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), fontSize = 15.sp)
                        StatusBadge(wo.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Type: ${wo.writeOffType} • Source: ${wo.sourceType}", fontSize = 12.sp, color = Color.LightGray)
                    Text("Reason: ${wo.reason}", fontSize = 13.sp, color = Color.White)
                    Text("Justification: ${wo.justification}", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Eligible: BDT ${wo.eligibleBalance}", fontSize = 12.sp, color = Color.Gray)
                        Text("Write-Off Amount: BDT ${wo.amount}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                    }

                    if (wo.status in setOf("REQUESTED", "UNDER_REVIEW")) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onApprove(wo.id, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Authorize Write-Off", fontSize = 11.sp)
                        }
                    } else if (wo.status == "APPROVED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onPost(wo.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Post Write-Off", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReversalsListView(
    adjustments: List<FinancialAdjustmentResponse>,
    onReverse: (String, String) -> Unit
) {
    if (adjustments.isEmpty()) {
        EmptyStateCard("No posted adjustments eligible for reversal.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(adjustments) { adj ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131D2E)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(adj.adjustmentNumber, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 15.sp)
                        StatusBadge(adj.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Adjustment: BDT ${adj.adjustmentAmount} (${adj.adjustmentType})", fontSize = 13.sp, color = Color.White)
                    if (adj.ledgerPostingId != null) {
                        Text("Ledger Posting: #${adj.ledgerPostingId}", fontSize = 11.sp, color = Color(0xFF81D4FA))
                    }
                    if (adj.reversingPostingId != null) {
                        Text("Reversing Posting: #${adj.reversingPostingId}", fontSize = 11.sp, color = Color(0xFFFF8A80))
                    }

                    if (adj.status == "POSTED") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { onReverse(adj.id, "Manager approved transaction reversal") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Reverse Transaction", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExceptionsListView(exceptions: List<FinancialExceptionResponse>) {
    if (exceptions.isEmpty()) {
        EmptyStateCard("No unresolved financial exceptions detected.")
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(exceptions) { ex ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF261414)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFB71C1C), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ex.issueType, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252), fontSize = 14.sp)
                        Text(ex.severity, fontSize = 11.sp, color = Color.White, modifier = Modifier.background(Color(0xFFB71C1C), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ex.description, fontSize = 12.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Ref: ${ex.referenceNumber} • Amount: BDT ${ex.amount}", fontSize = 11.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun AdjustmentAuditListView(auditEvents: List<FinancialAdjustmentAuditEventResponse>) {
    if (auditEvents.isEmpty()) {
        EmptyStateCard("No audit events recorded.")
        return
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(auditEvents) { event ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16202E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("${event.eventType} • ${event.entityType}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
                        Text("Actor: ${event.actorId} (${event.actorRole})", fontSize = 11.sp, color = Color.LightGray)
                        if (event.reason != null) {
                            Text("Note: ${event.reason}", fontSize = 11.sp, color = Color.Gray)
                        }
                        Text(dateFormat.format(Date(event.timestamp)), fontSize = 10.sp, color = Color(0xFF00E5FF))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "DRAFT" -> Color(0xFF263238) to Color(0xFF90A4AE)
        "SUBMITTED", "REQUESTED" -> Color(0xFF01579B) to Color(0xFF81D4FA)
        "UNDER_REVIEW" -> Color(0xFFE65100) to Color(0xFFFFB74D)
        "APPROVED" -> Color(0xFF1B5E20) to Color(0xFF81C784)
        "POSTED", "SETTLED" -> Color(0xFF4A148C) to Color(0xFFCE93D8)
        "RECONCILED" -> Color(0xFF004D40) to Color(0xFF80CBC4)
        "REVERSED", "REJECTED", "CANCELLED", "VOIDED" -> Color(0xFFB71C1C) to Color(0xFFFF8A80)
        else -> Color(0xFF263238) to Color.White
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = status, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
    ) {
        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = message, color = Color.Gray, fontSize = 14.sp)
        }
    }
}
