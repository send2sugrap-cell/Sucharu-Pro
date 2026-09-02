package com.sucharu.sucharupro.ui.features.customerfinancial

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
import com.sucharu.sucharupro.data.api.model.CreateCustomerFinancialReportScheduleRequest
import com.sucharu.sucharupro.data.api.model.CustomerFinancialReportScheduleDto
import com.sucharu.sucharupro.data.api.model.CustomerFinancialScheduleExecutionDto
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerFinancialReportSchedulesScreen(
    customerId: String,
    schedules: List<CustomerFinancialReportScheduleDto>,
    executions: List<CustomerFinancialScheduleExecutionDto>,
    onCreateSchedule: (CreateCustomerFinancialReportScheduleRequest) -> Unit,
    onPauseSchedule: (String) -> Unit,
    onResumeSchedule: (String) -> Unit,
    onCancelSchedule: (String) -> Unit,
    onViewExecutions: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedReportType by remember { mutableStateOf("CUSTOMER_STATEMENT") }
    var selectedFormat by remember { mutableStateOf("PDF") }
    var selectedFrequency by remember { mutableStateOf("MONTHLY") }
    var selectedTimezone by remember { mutableStateOf("Asia/Dhaka") }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheduled Financial Reports", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Schedule")
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
            if (schedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No active report delivery schedules.", color = Color.Gray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Delivery Schedule")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(schedules, key = { it.scheduleId }) { schedule ->
                        ScheduleCard(
                            schedule = schedule,
                            dateFormat = dateFormat,
                            onPause = { onPauseSchedule(schedule.scheduleId) },
                            onResume = { onResumeSchedule(schedule.scheduleId) },
                            onCancel = { onCancelSchedule(schedule.scheduleId) },
                            onViewExecutions = { onViewExecutions(schedule.scheduleId) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Scheduled Report Delivery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Report Type:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedReportType == "CUSTOMER_STATEMENT",
                            onClick = { selectedReportType = "CUSTOMER_STATEMENT" },
                            label = { Text("Statement") }
                        )
                        FilterChip(
                            selected = selectedReportType == "RECEIVABLE_AGING",
                            onClick = { selectedReportType = "RECEIVABLE_AGING" },
                            label = { Text("Aging") }
                        )
                        FilterChip(
                            selected = selectedReportType == "PAYMENT_HISTORY",
                            onClick = { selectedReportType = "PAYMENT_HISTORY" },
                            label = { Text("Payments") }
                        )
                    }

                    Text("Frequency:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFrequency == "DAILY",
                            onClick = { selectedFrequency = "DAILY" },
                            label = { Text("Daily") }
                        )
                        FilterChip(
                            selected = selectedFrequency == "WEEKLY",
                            onClick = { selectedFrequency = "WEEKLY" },
                            label = { Text("Weekly") }
                        )
                        FilterChip(
                            selected = selectedFrequency == "MONTHLY",
                            onClick = { selectedFrequency = "MONTHLY" },
                            label = { Text("Monthly") }
                        )
                    }

                    Text("Format:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedFormat == "PDF",
                            onClick = { selectedFormat = "PDF" },
                            label = { Text("PDF") }
                        )
                        FilterChip(
                            selected = selectedFormat == "CSV",
                            onClick = { selectedFormat = "CSV" },
                            label = { Text("CSV") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateSchedule(
                            CreateCustomerFinancialReportScheduleRequest(
                                reportType = selectedReportType,
                                format = selectedFormat,
                                frequency = selectedFrequency,
                                timezone = selectedTimezone
                            )
                        )
                        showCreateDialog = false
                    }
                ) {
                    Text("Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ScheduleCard(
    schedule: CustomerFinancialReportScheduleDto,
    dateFormat: SimpleDateFormat,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onViewExecutions: () -> Unit
) {
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
                Text(
                    text = schedule.reportType.replace("_", " "),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (schedule.status) {
                        "ACTIVE" -> Color(0xFF43A047).copy(alpha = 0.15f)
                        "PAUSED" -> Color(0xFFFB8C00).copy(alpha = 0.15f)
                        else -> Color.LightGray.copy(alpha = 0.3f)
                    }
                ) {
                    Text(
                        text = schedule.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = when (schedule.status) {
                            "ACTIVE" -> Color(0xFF2E7D32)
                            "PAUSED" -> Color(0xFFE65100)
                            else -> Color.DarkGray
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Frequency: ${schedule.frequency} • Format: ${schedule.format} • Timezone: ${schedule.timezone}",
                fontSize = 13.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Next Run: ${dateFormat.format(Date(schedule.nextRunAt))}",
                fontSize = 12.sp,
                color = Color(0xFF1E88E5)
            )

            val lastRunAt = schedule.lastRunAt
            if (lastRunAt != null) {
                Text(
                    text = "Last Run: ${dateFormat.format(Date(lastRunAt))} (${schedule.lastRunStatus ?: "UNKNOWN"})",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onViewExecutions) {
                    Text("Executions", fontSize = 12.sp)
                }

                if (schedule.status == "ACTIVE") {
                    TextButton(onClick = onPause) {
                        Text("Pause", fontSize = 12.sp)
                    }
                } else if (schedule.status == "PAUSED") {
                    TextButton(onClick = onResume) {
                        Text("Resume", fontSize = 12.sp)
                    }
                }

                if (schedule.status != "CANCELLED") {
                    TextButton(onClick = onCancel) {
                        Text("Cancel", fontSize = 12.sp, color = Color(0xFFE53935))
                    }
                }
            }
        }
    }
}
