package com.sucharu.sucharupro.ui.features.qc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import com.sucharu.sucharupro.domain.model.qc.QcDecision

/**
 * Screen displaying Quality Control Details, Inspector Assignment, and History (Module 06 Step 01).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionQcDetailsScreen(
    viewModel: ProductionQcDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কিউসি বিবরণ ও পর্যবেক্ষণ") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val state = uiState) {
                is ProductionQcDetailsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ProductionQcDetailsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ProductionQcDetailsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            ProductionQcHeaderCard(qc = state.qc)
                        }

                        if (state.assignments.isNotEmpty()) {
                            item {
                                Text(
                                    text = "ইন্সপেক্টর অ্যাসাইনমেন্ট হিস্টোরি (Inspector History)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(state.assignments, key = { it.assignmentId }) { assignment ->
                                QcAssignmentItemCard(assignment = assignment)
                            }
                        }

                        if (state.activities.isNotEmpty()) {
                            item {
                                Text(
                                    text = "অ্যাক্টিভিটি টাইমলাইন (Activity Timeline)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(state.activities, key = { it.eventId }) { activity ->
                                QcActivityItemCard(activity = activity)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionQcHeaderCard(
    qc: ProductionQc,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = qc.qcType.defaultLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                QcStatusBadge(status = qc.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "QC ID: ${qc.qcId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Job ID: ${qc.productionJobId}", style = MaterialTheme.typography.bodySmall)
            if (qc.productionStageId != null) {
                Text(text = "Stage ID: ${qc.productionStageId}", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Decision: ${qc.decision.defaultLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (qc.decision) {
                    QcDecision.PASS -> MaterialTheme.colorScheme.primary
                    QcDecision.FAIL -> MaterialTheme.colorScheme.error
                    QcDecision.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (qc.assignedInspectorId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Inspector: ${qc.assignedInspectorName ?: qc.assignedInspectorId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!qc.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notes: ${qc.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QcAssignmentItemCard(
    assignment: QcAssignment,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = assignment.inspectorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (assignment.isActive) "ACTIVE" else "REPLACED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (assignment.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Assigned by ${assignment.assignedBy ?: "Manager"} at ${assignment.assignedAt}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (assignment.unassignedAt != null) {
                Text(
                    text = "Unassigned at ${assignment.unassignedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun QcActivityItemCard(
    activity: QcActivityEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = activity.activityType.defaultLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val notes = activity.notes
            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = notes, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Logged by ${activity.actorName ?: activity.actorId ?: "System"} at ${activity.timestamp}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
