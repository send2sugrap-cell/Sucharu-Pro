package com.sucharu.sucharupro.ui.features.qc.reqc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus

/**
 * Re-QC Details Screen (Module 06 Step 06).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReQcDetailsScreen(
    viewModel: ReQcDetailsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(uiState.reQc?.let { "${it.reQcId} (Cycle ${it.cycleNumber})" } ?: "Re-QC Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val reQc = uiState.reQc
            if (reQc == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Re-QC record not found.", color = MaterialTheme.colorScheme.error)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    // Header card
                    ReQcHeaderCard(reQc = reQc)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Traceability card
                    ReQcTraceabilityCard(reQc = reQc)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Inspection Details card
                    ReQcInspectionInfoCard(reQc = reQc)

                    // Failure History card if any
                    if (uiState.failureRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReQcFailureHistoryCard(records = uiState.failureRecords)
                    }

                    // Cycle History card
                    if (uiState.cycleHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReQcCycleHistoryCard(cycles = uiState.cycleHistory, currentReQcId = reQc.reQcId)
                    }

                    // Activity Event log
                    if (uiState.activityEvents.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ReQcActivityLogCard(events = uiState.activityEvents)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ReQcHeaderCard(reQc: ReQcInspection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CYCLE ${reQc.cycleNumber}",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reQc.cycleType.defaultLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                ReQcStatusBadge(status = reQc.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Decision: ${reQc.decision.defaultLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when (reQc.decision) {
                        ReQcDecision.PASS -> Color(0xFF166534)
                        ReQcDecision.FAIL -> MaterialTheme.colorScheme.error
                        ReQcDecision.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                if (reQc.affectedQuantity != null) {
                    Text(
                        text = "Affected: ${reQc.affectedQuantity} ${reQc.quantityUnit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ReQcTraceabilityCard(reQc: ReQcInspection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Traceability & Lineage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Production Job: ${reQc.productionJobId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Project / Order: ${reQc.projectId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Source Rework: ${reQc.productionReworkId}", style = MaterialTheme.typography.bodySmall)
            if (reQc.originalDefectId != null) {
                Text(text = "Original Defect: ${reQc.originalDefectId}", style = MaterialTheme.typography.bodySmall)
            }
            if (reQc.originalQcId != null) {
                Text(text = "Original QC: ${reQc.originalQcId}", style = MaterialTheme.typography.bodySmall)
            }
            if (reQc.checklistId != null) {
                Text(text = "Checklist: ${reQc.checklistId}", style = MaterialTheme.typography.bodySmall)
            }
            if (reQc.previousReQcId != null) {
                Text(text = "Previous Cycle ID: ${reQc.previousReQcId}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReQcInspectionInfoCard(reQc: ReQcInspection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inspection Execution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Inspector: ${reQc.assignedInspectorName ?: reQc.assignedInspectorId ?: "Not assigned"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(text = "Created: ${reQc.createdAt} by ${reQc.createdByName ?: reQc.createdBy}", style = MaterialTheme.typography.bodySmall)
            if (reQc.startedAt != null) {
                Text(text = "Started: ${reQc.startedAt}", style = MaterialTheme.typography.bodySmall)
            }
            if (reQc.completedAt != null) {
                Text(text = "Completed: ${reQc.completedAt}", style = MaterialTheme.typography.bodySmall)
            }
            if (reQc.passNotes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pass Notes: ${reQc.passNotes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Medium
                )
            }
            val failureReason = reQc.failureReason
            if (failureReason != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Failure Reason: ${failureReason.defaultLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Failure Notes: ${reQc.failureNotes ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (reQc.returnedToReworkAt != null) {
                Text(
                    text = "Returned to Rework At: ${reQc.returnedToReworkAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A3412)
                )
            }
        }
    }
}

@Composable
private fun ReQcFailureHistoryCard(records: List<ReQcFailureRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Immutable Failure Records (${records.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            records.forEach { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Cycle ${record.cycleNumber} - ${record.failureReason.defaultLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = record.failureNotes,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Detected at: ${record.detectedAt} by ${record.detectedByName ?: record.detectedBy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReQcCycleHistoryCard(cycles: List<ReQcInspection>, currentReQcId: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cycle History (${cycles.size} cycles)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            cycles.forEach { cycle ->
                val isCurrent = cycle.reQcId == currentReQcId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cycle ${cycle.cycleNumber}: ${cycle.cycleType.defaultLabel} ${if (isCurrent) "(Active)" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "Rework: ${cycle.productionReworkId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ReQcStatusBadge(status = cycle.status)
                }
            }
        }
    }
}

@Composable
private fun ReQcActivityLogCard(events: List<ReQcActivityEvent>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Audit Log (${events.size} events)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.forEach { event ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = event.activityType.defaultLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = event.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val notes = event.notes
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
