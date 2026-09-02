package com.sucharu.sucharupro.ui.features.qc.costtime

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.qc.QcCostEntry
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import com.sucharu.sucharupro.domain.model.qc.QcCostType
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntry
import com.sucharu.sucharupro.domain.model.qc.QcTimeEntryType
import java.time.Instant

/**
 * Details screen displaying complete QC Cost & Time Reconciliation for a production job (Module 06 Step 08).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcCostTimeDetailsScreen(
    viewModel: QcCostTimeDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Job ${uiState.productionJobId} Reconciliation") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Reconciliation Overview Card
                item {
                    OverviewCard(reconciliation = uiState.reconciliation)
                }

                // 2. Action Controls
                item {
                    ActionButtonsRow(
                        uiState = uiState,
                        onRecordCost = viewModel::onRecordCostClicked,
                        onRecordTime = viewModel::onRecordTimeClicked,
                        onReconcile = viewModel::onReconcileClicked,
                        onAdjust = viewModel::onAdjustClicked,
                        onLock = viewModel::onLockClicked
                    )
                }

                // 3. Failure & Operational Counts
                uiState.reconciliation?.let { recon ->
                    item {
                        CountsCard(recon)
                    }
                }

                // 4. Cost Entries Section Header
                item {
                    Text("QC Cost Entries (${uiState.costEntries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (uiState.costEntries.isEmpty()) {
                    item {
                        Text("No QC cost entries recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(uiState.costEntries, key = { it.id }) { cost ->
                        CostEntryItemCard(cost)
                    }
                }

                // 5. Time Entries Section Header
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("QC Time Entries (${uiState.timeEntries.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                if (uiState.timeEntries.isEmpty()) {
                    item {
                        Text("No QC time tracking entries recorded yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(uiState.timeEntries, key = { it.id }) { time ->
                        TimeEntryItemCard(time)
                    }
                }
            }
        }

        // Dialogs
        if (uiState.showRecordCostDialog) {
            RecordCostDialog(
                onDismiss = viewModel::dismissDialogs,
                onConfirm = { type, desc, qty, unitCost ->
                    val now = Instant.now().toString()
                    viewModel.recordCost(
                        projectId = uiState.reconciliation?.projectId ?: "PRJ-DEFAULT",
                        costType = type,
                        description = desc,
                        quantity = qty,
                        unitCost = unitCost,
                        timestamp = now
                    )
                }
            )
        }

        if (uiState.showRecordTimeDialog) {
            RecordTimeDialog(
                onDismiss = viewModel::dismissDialogs,
                onConfirm = { type, duration, notes ->
                    val now = Instant.now().toString()
                    viewModel.recordTime(
                        projectId = uiState.reconciliation?.projectId ?: "PRJ-DEFAULT",
                        entryType = type,
                        startedAt = now,
                        endedAt = now,
                        durationMinutes = duration,
                        notes = notes,
                        timestamp = now
                    )
                }
            )
        }

        if (uiState.showReconcileDialog) {
            ReconcileDialog(
                currentPlannedCost = uiState.reconciliation?.plannedCost ?: 0.0,
                currentPlannedMinutes = uiState.reconciliation?.plannedMinutes ?: 0L,
                onDismiss = viewModel::dismissDialogs,
                onConfirm = { pCost, pMins, notes ->
                    val now = Instant.now().toString()
                    viewModel.executeReconciliation(pCost, pMins, notes, now)
                }
            )
        }

        if (uiState.showAdjustDialog && uiState.reconciliation != null) {
            AdjustDialog(
                reconciliation = uiState.reconciliation!!,
                onDismiss = viewModel::dismissDialogs,
                onConfirm = { pCost, pMins, reason ->
                    val now = Instant.now().toString()
                    viewModel.executeAdjustment(uiState.reconciliation!!.id, pCost, pMins, reason, now)
                }
            )
        }

        if (uiState.showLockDialog && uiState.reconciliation != null) {
            LockDialog(
                onDismiss = viewModel::dismissDialogs,
                onConfirm = { notes ->
                    val now = Instant.now().toString()
                    viewModel.executeLock(uiState.reconciliation!!.id, notes, now)
                }
            )
        }
    }
}

@Composable
private fun OverviewCard(reconciliation: QcCostTimeReconciliation?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Reconciliation Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = when (reconciliation?.status) {
                        QcCostStatus.LOCKED -> MaterialTheme.colorScheme.secondaryContainer
                        QcCostStatus.RECONCILED -> MaterialTheme.colorScheme.primaryContainer
                        QcCostStatus.ADJUSTED -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = reconciliation?.status?.defaultLabel ?: "NOT RECONCILED",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (reconciliation == null) {
                Text("No reconciliation performed yet for this job.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Planned Cost", style = MaterialTheme.typography.labelSmall)
                        Text("${reconciliation.plannedCost} BDT", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Actual Cost", style = MaterialTheme.typography.labelSmall)
                        Text("${reconciliation.actualCost} BDT", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Cost Variance: ${if (reconciliation.costVariance >= 0) "+" else ""}${reconciliation.costVariance} BDT",
                            color = if (reconciliation.hasCostOverrun) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Planned Time", style = MaterialTheme.typography.labelSmall)
                        Text("${reconciliation.plannedMinutes} mins", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Actual Time", style = MaterialTheme.typography.labelSmall)
                        Text("${reconciliation.actualMinutes} mins", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Time Variance: ${if (reconciliation.timeVarianceMinutes >= 0) "+" else ""}${reconciliation.timeVarianceMinutes}m",
                            color = if (reconciliation.hasTimeOverrun) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountsCard(reconciliation: QcCostTimeReconciliation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quality Effort Drivers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Defects Detected: ${reconciliation.defectCount}", style = MaterialTheme.typography.bodySmall)
                Text("Reworks Executed: ${reconciliation.reworkCount}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Re-QC Cycles: ${reconciliation.reQcCycleCount}", style = MaterialTheme.typography.bodySmall)
                Text("Final QC Inspections: ${reconciliation.finalQcCount}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    uiState: QcCostTimeDetailsUiState,
    onRecordCost: () -> Unit,
    onRecordTime: () -> Unit,
    onReconcile: () -> Unit,
    onAdjust: () -> Unit,
    onLock: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.canRecordCostOrTime) {
                OutlinedButton(
                    onClick = onRecordCost,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Add Cost")
                }
                OutlinedButton(
                    onClick = onRecordTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Add Time")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.canReconcile) {
                Button(
                    onClick = onReconcile,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Reconcile")
                }
            }

            if (uiState.canAdjust) {
                OutlinedButton(
                    onClick = onAdjust,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Adjust")
                }
            }

            if (uiState.canLock) {
                Button(
                    onClick = onLock,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Lock")
                }
            }
        }
    }
}

@Composable
private fun CostEntryItemCard(cost: QcCostEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cost.costType.defaultLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("${cost.totalCost} ${cost.currency}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            Text(cost.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Qty: ${cost.quantity} × ${cost.unitCost} | Status: ${cost.status.defaultLabel}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TimeEntryItemCard(time: QcTimeEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(time.entryType.defaultLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("${time.durationMinutes} mins", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
            time.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Actor: ${time.actorName ?: time.actorId} | Status: ${time.status.defaultLabel}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordCostDialog(
    onDismiss: () -> Unit,
    onConfirm: (QcCostType, String, Double, Double) -> Unit
) {
    var selectedType by remember { mutableStateOf(QcCostType.INSPECTION) }
    var description by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1.0") }
    var unitCostStr by remember { mutableStateOf("0.0") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record QC Operational Cost") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedType.defaultLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Cost Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        QcCostType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.defaultLabel) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = unitCostStr,
                    onValueChange = { unitCostStr = it },
                    label = { Text("Unit Cost (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val unitCost = unitCostStr.toDoubleOrNull() ?: 0.0
                    onConfirm(selectedType, description, qty, unitCost)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTimeDialog(
    onDismiss: () -> Unit,
    onConfirm: (QcTimeEntryType, Long, String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(QcTimeEntryType.INSPECTION) }
    var durationStr by remember { mutableStateOf("30") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record QC Time Tracking") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedType.defaultLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Activity Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        QcTimeEntryType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.defaultLabel) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationStr.toLongOrNull() ?: 0L
                    onConfirm(selectedType, duration, notes.ifBlank { null })
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ReconcileDialog(
    currentPlannedCost: Double,
    currentPlannedMinutes: Long,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long, String?) -> Unit
) {
    var plannedCostStr by remember { mutableStateOf(currentPlannedCost.toString()) }
    var plannedMinutesStr by remember { mutableStateOf(currentPlannedMinutes.toString()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculate QC Reconciliation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = plannedCostStr,
                    onValueChange = { plannedCostStr = it },
                    label = { Text("Planned QC Cost (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = plannedMinutesStr,
                    onValueChange = { plannedMinutesStr = it },
                    label = { Text("Planned QC Time (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Reconciliation Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pCost = plannedCostStr.toDoubleOrNull() ?: 0.0
                    val pMins = plannedMinutesStr.toLongOrNull() ?: 0L
                    onConfirm(pCost, pMins, notes.ifBlank { null })
                }
            ) {
                Text("Reconcile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AdjustDialog(
    reconciliation: QcCostTimeReconciliation,
    onDismiss: () -> Unit,
    onConfirm: (Double?, Long?, String) -> Unit
) {
    var adjustedCostStr by remember { mutableStateOf(reconciliation.plannedCost.toString()) }
    var adjustedMinutesStr by remember { mutableStateOf(reconciliation.plannedMinutes.toString()) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Reconciliation Benchmark") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = adjustedCostStr,
                    onValueChange = { adjustedCostStr = it },
                    label = { Text("Adjusted Planned Cost (BDT)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = adjustedMinutesStr,
                    onValueChange = { adjustedMinutesStr = it },
                    label = { Text("Adjusted Planned Time (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Adjustment Reason (Mandatory)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = reason.isNotBlank(),
                onClick = {
                    val pCost = adjustedCostStr.toDoubleOrNull()
                    val pMins = adjustedMinutesStr.toLongOrNull()
                    onConfirm(pCost, pMins, reason)
                }
            ) {
                Text("Apply Adjustment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LockDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permanently Lock Reconciliation?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Locking is permanent. Once locked, all cost entries, time tracking entries, and reconciliation figures will be sealed and immutable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lock Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = { onConfirm(notes.ifBlank { null }) }
            ) {
                Text("Lock Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
