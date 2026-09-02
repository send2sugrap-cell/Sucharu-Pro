package com.sucharu.sucharupro.ui.features.qc.checklist

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus

/**
 * Screen displaying the active QC Inspection Checklist execution (Module 06 Step 03).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcInspectionChecklistScreen(
    viewModel: QcInspectionChecklistViewModel,
    onNavigateBack: () -> Unit,
    currentUserId: String = "inspector-01",
    currentUserName: String = "ইন্সপেক্টর",
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কিউসি ইন্সপেকশন চেকলিস্ট") },
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
                is QcInspectionChecklistUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is QcInspectionChecklistUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
                is QcInspectionChecklistUiState.Success -> {
                    val responseMap = state.responses.associateBy { it.checklistItemId }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                                            text = "Inspection Checklist",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = state.checklist.status.defaultLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Job ID: ${state.checklist.productionJobId}", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Template Version: V${state.checklist.checklistTemplateVersion}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        if (state.checklist.status == QcChecklistStatus.READY) {
                            item {
                                Button(
                                    onClick = { viewModel.startChecklist("2026-08-16T10:00:00Z") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Start Checklist Execution")
                                }
                            }
                        }

                        item {
                            Text(
                                text = "ইন্সপেকশন আইটেমসমূহ (${state.responses.count { it.isEvaluated }}/${state.items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(state.items, key = { it.itemId }) { item ->
                            val currentResp = responseMap[item.itemId]
                            InspectionChecklistItemCard(
                                item = item,
                                currentResponse = currentResp,
                                isEditable = state.checklist.isEditable,
                                onStatusChange = { newStatus ->
                                    viewModel.saveResponse(
                                        inspectionId = state.checklist.inspectionId,
                                        checklistItemId = item.itemId,
                                        status = newStatus,
                                        value = null,
                                        numericValue = null,
                                        selectedValue = null,
                                        remarks = if (newStatus == QcResponseStatus.FAIL) "Defect detected" else null,
                                        respondedBy = currentUserId,
                                        respondedByName = currentUserName,
                                        timestamp = "2026-08-16T10:00:00Z"
                                    )
                                }
                            )
                        }

                        if (state.checklist.isEditable && state.checklist.status == QcChecklistStatus.IN_PROGRESS) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.completeChecklist(
                                                decision = QcDecision.PASS,
                                                completedBy = currentUserId,
                                                completedByName = currentUserName,
                                                notes = "Checklist passed successfully.",
                                                timestamp = "2026-08-16T10:00:00Z"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("PASS")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.completeChecklist(
                                                decision = QcDecision.FAIL,
                                                completedBy = currentUserId,
                                                completedByName = currentUserName,
                                                notes = "Checklist failed criteria.",
                                                timestamp = "2026-08-16T10:00:00Z"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("FAIL")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InspectionChecklistItemCard(
    item: QcChecklistItem,
    currentResponse: QcInspectionResponse?,
    isEditable: Boolean,
    onStatusChange: (QcResponseStatus) -> Unit,
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
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentResponse?.status?.defaultLabel ?: "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            val instructions = item.instructions
            if (!instructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isEditable) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentResponse?.status == QcResponseStatus.PASS,
                        onClick = { onStatusChange(QcResponseStatus.PASS) },
                        label = { Text("PASS") }
                    )
                    FilterChip(
                        selected = currentResponse?.status == QcResponseStatus.FAIL,
                        onClick = { onStatusChange(QcResponseStatus.FAIL) },
                        label = { Text("FAIL") }
                    )
                    FilterChip(
                        selected = currentResponse?.status == QcResponseStatus.NOT_APPLICABLE,
                        onClick = { onStatusChange(QcResponseStatus.NOT_APPLICABLE) },
                        label = { Text("N/A") }
                    )
                }
            }
        }
    }
}
