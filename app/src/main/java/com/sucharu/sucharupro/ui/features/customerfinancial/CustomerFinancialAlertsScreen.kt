package com.sucharu.sucharupro.ui.features.customerfinancial

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.data.api.model.CustomerFinancialAlertDto
import com.sucharu.sucharupro.data.api.model.CustomerFinancialAlertSummaryDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFinancialAlertsScreen(
    customerId: String,
    alerts: List<CustomerFinancialAlertDto>,
    summary: CustomerFinancialAlertSummaryDto?,
    onEvaluateAlerts: () -> Unit,
    onAcknowledgeAlert: (String) -> Unit,
    onResolveAlert: (String, String) -> Unit,
    onDismissAlert: (String, String) -> Unit,
    onViewAuditTrail: (String) -> Unit,
    isStaff: Boolean = true
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var dismissingAlertId by remember { mutableStateOf<String?>(null) }
    var dismissalReason by remember { mutableStateOf("") }
    var resolvingAlertId by remember { mutableStateOf<String?>(null) }
    var resolutionReason by remember { mutableStateOf("") }

    val filteredAlerts = alerts.filter { alert ->
        val matchesFilter = when (selectedFilter) {
            "OPEN" -> alert.status == "OPEN"
            "ACKNOWLEDGED" -> alert.status == "ACKNOWLEDGED"
            "URGENT" -> alert.severity in listOf("CRITICAL", "HIGH")
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                alert.title.contains(searchQuery, ignoreCase = true) ||
                alert.safeMessage.contains(searchQuery, ignoreCase = true) ||
                alert.alertType.contains(searchQuery, ignoreCase = true)

        matchesFilter && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Financial Alerts & Signals", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onEvaluateAlerts) {
                        Icon(Icons.Default.Refresh, contentDescription = "Evaluate Alerts")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // KPI Summary Cards
            if (summary != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlertKpiCard(
                        title = "Critical",
                        count = summary.criticalCount,
                        color = Color(0xFFE53935),
                        modifier = Modifier.weight(1f)
                    )
                    AlertKpiCard(
                        title = "High",
                        count = summary.highCount,
                        color = Color(0xFFFB8C00),
                        modifier = Modifier.weight(1f)
                    )
                    AlertKpiCard(
                        title = "Total Open",
                        count = summary.totalOpen,
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                    AlertKpiCard(
                        title = "Resolved",
                        count = summary.resolvedCount,
                        color = Color(0xFF43A047),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search and Filter Chips
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search alerts") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text("All (${alerts.size})") }
                )
                FilterChip(
                    selected = selectedFilter == "URGENT",
                    onClick = { selectedFilter = "URGENT" },
                    label = { Text("Urgent") }
                )
                FilterChip(
                    selected = selectedFilter == "OPEN",
                    onClick = { selectedFilter = "OPEN" },
                    label = { Text("Open") }
                )
                FilterChip(
                    selected = selectedFilter == "ACKNOWLEDGED",
                    onClick = { selectedFilter = "ACKNOWLEDGED" },
                    label = { Text("Acknowledged") }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Alert List
            if (filteredAlerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No financial alerts found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAlerts, key = { it.alertId }) { alert ->
                        AlertCard(
                            alert = alert,
                            isStaff = isStaff,
                            onAcknowledge = { onAcknowledgeAlert(alert.alertId) },
                            onResolve = { resolvingAlertId = alert.alertId },
                            onDismiss = { dismissingAlertId = alert.alertId },
                            onViewAudit = { onViewAuditTrail(alert.alertId) }
                        )
                    }
                }
            }
        }
    }

    // Dismissal Dialog
    if (dismissingAlertId != null) {
        AlertDialog(
            onDismissRequest = { dismissingAlertId = null },
            title = { Text("Dismiss Financial Alert") },
            text = {
                Column {
                    Text("Please provide a mandatory reason for dismissing this alert:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = dismissalReason,
                        onValueChange = { dismissalReason = it },
                        label = { Text("Dismissal Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dismissalReason.isNotBlank()) {
                            onDismissAlert(dismissingAlertId!!, dismissalReason)
                            dismissingAlertId = null
                            dismissalReason = ""
                        }
                    },
                    enabled = dismissalReason.isNotBlank()
                ) {
                    Text("Dismiss Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissingAlertId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Resolution Dialog
    if (resolvingAlertId != null) {
        AlertDialog(
            onDismissRequest = { resolvingAlertId = null },
            title = { Text("Resolve Financial Alert") },
            text = {
                Column {
                    Text("Enter resolution details:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resolutionReason,
                        onValueChange = { resolutionReason = it },
                        label = { Text("Resolution Reason / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveAlert(resolvingAlertId!!, resolutionReason.ifBlank { "Resolved by operator" })
                        resolvingAlertId = null
                        resolutionReason = ""
                    }
                ) {
                    Text("Mark Resolved")
                }
            },
            dismissButton = {
                TextButton(onClick = { resolvingAlertId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AlertKpiCard(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AlertCard(
    alert: CustomerFinancialAlertDto,
    isStaff: Boolean,
    onAcknowledge: () -> Unit,
    onResolve: () -> Unit,
    onDismiss: () -> Unit,
    onViewAudit: () -> Unit
) {
    val severityColor = when (alert.severity) {
        "CRITICAL" -> Color(0xFFE53935)
        "HIGH" -> Color(0xFFFB8C00)
        "MEDIUM" -> Color(0xFFFDD835)
        "LOW" -> Color(0xFF43A047)
        else -> Color(0xFF1E88E5)
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = severityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = alert.severity,
                        color = severityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (alert.status) {
                        "OPEN" -> Color(0xFF1E88E5).copy(alpha = 0.15f)
                        "ACKNOWLEDGED" -> Color(0xFF8E24AA).copy(alpha = 0.15f)
                        "RESOLVED" -> Color(0xFF43A047).copy(alpha = 0.15f)
                        else -> Color.LightGray.copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        text = alert.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = alert.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = alert.safeMessage, fontSize = 13.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Detected: ${dateFormat.format(Date(alert.detectedAt))}",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                val dueAt = alert.dueAt
                if (dueAt != null) {
                    Text(
                        text = "Due: ${dateFormat.format(Date(dueAt))}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            if (alert.status in listOf("OPEN", "ACKNOWLEDGED")) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onViewAudit) {
                        Text("Audit", fontSize = 12.sp)
                    }

                    if (alert.status == "OPEN") {
                        TextButton(onClick = onAcknowledge) {
                            Text("Acknowledge", fontSize = 12.sp)
                        }
                    }

                    if (isStaff) {
                        TextButton(onClick = onResolve) {
                            Text("Resolve", fontSize = 12.sp)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Dismiss", fontSize = 12.sp, color = Color(0xFFE53935))
                        }
                    }
                }
            }
        }
    }
}
