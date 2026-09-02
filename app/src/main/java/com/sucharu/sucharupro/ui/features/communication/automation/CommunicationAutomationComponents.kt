package com.sucharu.sucharupro.ui.features.communication.automation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.communication.automation.AutomationDecisionType
import com.sucharu.sucharupro.domain.model.communication.automation.AutomationExecutionStatus
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationExecution
import com.sucharu.sucharupro.domain.model.communication.automation.CommunicationAutomationRule

val AutoBg = Color(0xFF0F172A)
val AutoSurface = Color(0xFF1E293B)
val AutoAccent = Color(0xFF38BDF8)
val AutoAccentGreen = Color(0xFF22C55E)
val AutoAccentAmber = Color(0xFFF59E0B)
val AutoAccentRed = Color(0xFFEF4444)
val AutoAccentPurple = Color(0xFFA855F7)
val AutoTextPrimary = Color(0xFFF1F5F9)
val AutoTextSecondary = Color(0xFF94A3B8)
val AutoBorder = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, color = AutoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AutoTextPrimary)
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AutoSurface)
    )
}

@Composable
fun AutomationExecutionStatusBadge(status: AutomationExecutionStatus) {
    val (bg, fg) = when (status) {
        AutomationExecutionStatus.RECEIVED -> Color(0xFF334155) to Color(0xFF94A3B8)
        AutomationExecutionStatus.EVALUATING -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        AutomationExecutionStatus.MATCHED -> Color(0xFF0C4A6E) to Color(0xFF38BDF8)
        AutomationExecutionStatus.QUEUED, AutomationExecutionStatus.SCHEDULED -> Color(0xFF78350F) to Color(0xFFFBBF24)
        AutomationExecutionStatus.DISPATCHING -> Color(0xFF581C87) to Color(0xFFC084FC)
        AutomationExecutionStatus.DISPATCHED, AutomationExecutionStatus.COMPLETED -> Color(0xFF065F46) to Color(0xFF10B981)
        AutomationExecutionStatus.SUPPRESSED -> Color(0xFF4B5563) to Color(0xFF9CA3AF)
        AutomationExecutionStatus.FAILED -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        AutomationExecutionStatus.CANCELLED -> Color(0xFF374151) to Color(0xFF9CA3AF)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(status.defaultLabel, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AutomationDecisionChip(decisionType: AutomationDecisionType) {
    val (bg, fg) = when (decisionType) {
        AutomationDecisionType.SEND -> Color(0xFF064E3B) to Color(0xFF34D399)
        AutomationDecisionType.SCHEDULE -> Color(0xFF1E3A8A) to Color(0xFF60A5FA)
        AutomationDecisionType.SUPPRESS -> Color(0xFF4B5563) to Color(0xFF9CA3AF)
        AutomationDecisionType.ESCALATE -> Color(0xFF7F1D1D) to Color(0xFFF87171)
        else -> Color(0xFF334155) to Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(decisionType.defaultLabel, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AutomationMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AutoSurface),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = AutoTextSecondary, fontSize = 12.sp)
                Text(value, color = AutoTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AutomationRuleCard(
    rule: CommunicationAutomationRule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AutoSurface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(rule.ruleNo, color = AutoTextSecondary, fontSize = 12.sp)
                Box(
                    modifier = Modifier
                        .background(if (rule.enabled) Color(0xFF065F46) else Color(0xFF374151), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(if (rule.enabled) "ACTIVE" else "DISABLED", color = if (rule.enabled) Color(0xFF10B981) else Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(rule.name, color = AutoTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (rule.description.isNotBlank()) {
                Text(rule.description, color = AutoTextSecondary, fontSize = 13.sp, maxLines = 2)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = AutoAccent, modifier = Modifier.size(14.dp))
                    Text(rule.eventType.defaultLabel, color = AutoTextPrimary, fontSize = 12.sp)
                }
                Text("${rule.conditions.size} condition(s)", color = AutoTextSecondary, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AutomationExecutionCard(
    execution: CommunicationAutomationExecution,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AutoSurface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(execution.executionNo, color = AutoTextSecondary, fontSize = 12.sp)
                AutomationExecutionStatusBadge(execution.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutomationDecisionChip(execution.decision.decisionType)
                if (execution.recipientUserId != null) {
                    Text("To: ${execution.recipientUserId}", color = AutoTextSecondary, fontSize = 11.sp)
                }
            }

            val title = execution.decision.renderedTitle
            if (title != null) {
                Text(title, color = AutoTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
