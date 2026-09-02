package com.sucharu.sucharupro.ui.features.qc.preproduction

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.ui.features.qc.QcStatusBadge

/**
 * Screen displaying the Pre-Production Quality Control checklist, specifications, and submission (Module 06 Step 02).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreProductionQcScreen(
    viewModel: PreProductionQcViewModel,
    onNavigateBack: () -> Unit,
    currentUserId: String = "inspector-01",
    currentUserName: String = "ইন্সপেক্টর",
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রি-প্রোডাকশন কিউসি চেকলিস্ট") },
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
                is PreProductionQcUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PreProductionQcUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }
                is PreProductionQcUiState.Success -> {
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
                                            text = "Pre-Production QC",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        QcStatusBadge(status = state.qc.status)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "Job ID: ${state.qc.productionJobId}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = "Decision: ${state.qc.decision.defaultLabel}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (state.qc.decision) {
                                            QcDecision.PASS -> MaterialTheme.colorScheme.primary
                                            QcDecision.FAIL -> MaterialTheme.colorScheme.error
                                            QcDecision.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "ম্যানুফ্যাকচারিং ভেরিফিকেশন চেকলিস্ট (${state.items.count { it.isEvaluated }}/${state.items.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(state.items, key = { it.itemId }) { item ->
                            PreProductionQcItemRow(
                                item = item,
                                isEditable = state.qc.isEditable,
                                onStatusChange = { newStatus ->
                                    viewModel.updateItemStatus(
                                        itemId = item.itemId,
                                        status = newStatus,
                                        notes = null,
                                        checkedBy = currentUserId,
                                        checkedByName = currentUserName,
                                        timestamp = "2026-08-16T10:00:00Z"
                                    )
                                }
                            )
                        }

                        if (state.qc.isEditable) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.submitQc(
                                                decision = QcDecision.PASS,
                                                snapshot = null,
                                                submittedBy = currentUserId,
                                                submittedByName = currentUserName,
                                                notes = "Pre-Production QC passed successfully.",
                                                timestamp = "2026-08-16T10:00:00Z"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("PASS QC")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.submitQc(
                                                decision = QcDecision.FAIL,
                                                snapshot = null,
                                                submittedBy = currentUserId,
                                                submittedByName = currentUserName,
                                                notes = "Pre-Production QC failed requirements.",
                                                timestamp = "2026-08-16T10:00:00Z"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("FAIL QC")
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
fun PreProductionQcItemRow(
    item: PreProductionQcItem,
    isEditable: Boolean,
    onStatusChange: (PreProductionItemStatus) -> Unit,
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
                    text = item.category.defaultLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                ItemStatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.label, style = MaterialTheme.typography.bodySmall)

            if (isEditable) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = item.status == PreProductionItemStatus.PASS,
                        onClick = { onStatusChange(PreProductionItemStatus.PASS) },
                        label = { Text("PASS") }
                    )
                    FilterChip(
                        selected = item.status == PreProductionItemStatus.FAIL,
                        onClick = { onStatusChange(PreProductionItemStatus.FAIL) },
                        label = { Text("FAIL") }
                    )
                    FilterChip(
                        selected = item.status == PreProductionItemStatus.NOT_APPLICABLE,
                        onClick = { onStatusChange(PreProductionItemStatus.NOT_APPLICABLE) },
                        label = { Text("N/A") }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemStatusBadge(status: PreProductionItemStatus) {
    val (bgColor, textColor) = when (status) {
        PreProductionItemStatus.PENDING -> Pair(Color(0xFFE2E8F0), Color(0xFF475569))
        PreProductionItemStatus.PASS -> Pair(Color(0xFFD1FAE5), Color(0xFF065F46))
        PreProductionItemStatus.FAIL -> Pair(Color(0xFFFEE2E2), Color(0xFF991B1B))
        PreProductionItemStatus.NOT_APPLICABLE -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
    }

    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.defaultLabel,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
