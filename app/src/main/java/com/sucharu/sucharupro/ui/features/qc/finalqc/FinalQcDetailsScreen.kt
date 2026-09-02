package com.sucharu.sucharupro.ui.features.qc.finalqc

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Detailed view and quality gate controller for a Final QC inspection record (Module 06 Step 07).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalQcDetailsScreen(
    viewModel: FinalQcDetailsViewModel,
    onNavigateBack: () -> Unit,
    currentUserId: String = "user-admin",
    currentUserName: String = "Admin User",
    currentUserRole: UserRole = UserRole.ADMIN,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.inspection?.finalQcId ?: "Final QC Details",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    state.inspection?.let {
                        FinalQcStatusBadge(
                            status = it.status,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.inspection == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Final QC inspection record not found.")
                }
            }
            else -> {
                val inspection = state.inspection!!
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Overview Card
                    item {
                        FinalQcOverviewCard(inspection = inspection)
                    }

                    // 2. Action Controls
                    item {
                        FinalQcActionButtons(
                            inspection = inspection,
                            isEligibleForRelease = state.eligibilityResult?.isEligible == true,
                            onStart = {
                                viewModel.startInspection(
                                    inspectorId = currentUserId,
                                    inspectorName = currentUserName,
                                    callerRole = currentUserRole
                                )
                            },
                            onPass = { viewModel.setShowPassDialog(true) },
                            onFail = { viewModel.setShowFailDialog(true) },
                            onAuthorizeRelease = { viewModel.setShowReleaseDialog(true) }
                        )
                    }

                    // 3. 14-Point Quality Gate & Release Eligibility Card
                    item {
                        FinalQcEligibilityCard(
                            eligibilityResult = state.eligibilityResult,
                            onRefresh = { viewModel.checkEligibility() }
                        )
                    }

                    // 4. Release Authorization Card (if released)
                    if (inspection.isReleased || state.releaseAuthorization != null) {
                        item {
                            FinalQcReleaseAuthorizationCard(
                                authorization = state.releaseAuthorization,
                                inspection = inspection
                            )
                        }
                    }

                    // 5. Lineage & Quality Traceability Card
                    item {
                        FinalQcTraceabilityCard(inspection = inspection)
                    }

                    // 6. Audit History
                    item {
                        FinalQcAuditCard(events = state.activityEvents)
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    // Dialogs
    if (state.showPassDialog) {
        FinalQcPassDialog(
            totalQuantity = state.inspection?.totalQuantity ?: 0,
            onDismiss = { viewModel.setShowPassDialog(false) },
            onConfirm = { acceptedQty, notes ->
                viewModel.passInspection(
                    acceptedQuantity = acceptedQty,
                    notes = notes,
                    inspectorId = currentUserId,
                    inspectorName = currentUserName,
                    callerRole = currentUserRole
                )
            }
        )
    }

    if (state.showFailDialog) {
        FinalQcFailDialog(
            totalQuantity = state.inspection?.totalQuantity ?: 0,
            onDismiss = { viewModel.setShowFailDialog(false) },
            onConfirm = { rejectedQty, reason, notes ->
                viewModel.failInspection(
                    rejectedQuantity = rejectedQty,
                    failureReason = reason,
                    notes = notes,
                    inspectorId = currentUserId,
                    inspectorName = currentUserName,
                    callerRole = currentUserRole
                )
            }
        )
    }

    if (state.showReleaseDialog) {
        FinalQcReleaseDialog(
            onDismiss = { viewModel.setShowReleaseDialog(false) },
            onConfirm = { notes ->
                viewModel.authorizeRelease(
                    releaseNotes = notes,
                    authorizedBy = currentUserId,
                    authorizedByName = currentUserName,
                    callerRole = currentUserRole
                )
            }
        )
    }
}

@Composable
fun FinalQcOverviewCard(
    inspection: FinalQcInspection,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inspection Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Job ID", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = inspection.productionJobId, fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Project ID", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = inspection.projectId, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Total Quantity", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(text = "${inspection.totalQuantity} ${inspection.quantityUnit}", fontWeight = FontWeight.SemiBold)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Accepted / Rejected", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${inspection.acceptedQuantity} / ${inspection.rejectedQuantity}",
                        fontWeight = FontWeight.Bold,
                        color = if (inspection.rejectedQuantity > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
            }

            val notes = inspection.notes
            if (!notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "Notes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(text = notes, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FinalQcActionButtons(
    inspection: FinalQcInspection,
    isEligibleForRelease: Boolean,
    onStart: () -> Unit,
    onPass: () -> Unit,
    onFail: () -> Unit,
    onAuthorizeRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Inspection Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (inspection.status in setOf(FinalQcStatus.DRAFT, FinalQcStatus.PENDING, FinalQcStatus.ASSIGNED, FinalQcStatus.BLOCKED)) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Inspection")
                    }
                }

                if (inspection.status == FinalQcStatus.IN_INSPECTION) {
                    Button(
                        onClick = onPass,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASS")
                    }

                    Button(
                        onClick = onFail,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FAIL")
                    }
                }
            }

            // Production Release Authorization Button
            if (inspection.status == FinalQcStatus.PASSED && !inspection.isReleased) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAuthorizeRelease,
                    enabled = isEligibleForRelease,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Authorize Production Release", fontWeight = FontWeight.Bold)
                }
                if (!isEligibleForRelease) {
                    Text(
                        text = "Production release is blocked by one or more quality gate conditions below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FinalQcEligibilityCard(
    eligibilityResult: com.sucharu.sucharupro.domain.model.qc.FinalQcEligibilityResult?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (eligibilityResult?.isEligible == true) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (eligibilityResult?.isEligible == true) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (eligibilityResult?.isEligible == true) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quality Gate & Release Eligibility",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (eligibilityResult?.isEligible == true) Color(0xFF166534) else Color(0xFF991B1B)
                    )
                }
                TextButton(onClick = onRefresh) {
                    Text("Re-evaluate")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (eligibilityResult != null) {
                Text(
                    text = eligibilityResult.message,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (eligibilityResult.isEligible) Color(0xFF166534) else Color(0xFF991B1B)
                )

                if (!eligibilityResult.isEligible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        eligibilityResult.reasons.forEach { reason ->
                            Text(
                                text = "• ${reason.defaultLabel}",
                                fontSize = 12.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }
            } else {
                Text(text = "Evaluating release eligibility...", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun FinalQcReleaseAuthorizationCard(
    authorization: FinalQcReleaseAuthorization?,
    inspection: FinalQcInspection,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4338CA)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Production Release Authorization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3730A3)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Authorization ID: ${authorization?.releaseAuthorizationId ?: inspection.releaseAuthorizationId ?: "N/A"}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Authorized By: ${authorization?.authorizedByName ?: authorization?.authorizedBy ?: "Management"}",
                fontSize = 12.sp
            )
            Text(
                text = "Timestamp: ${authorization?.authorizedAt ?: inspection.updatedAt}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            if (!authorization?.releaseNotes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Release Notes: ${authorization?.releaseNotes}",
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun FinalQcTraceabilityCard(
    inspection: FinalQcInspection,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quality Lineage & Traceability",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pre-Production QC: ${inspection.preProductionQcId ?: "Passed (Standard)"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Checklist ID: ${inspection.checklistId ?: "Standard Checklist Verified"}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Source Defects: ${if (inspection.sourceDefectIds.isEmpty()) "None (Clean)" else inspection.sourceDefectIds.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Source Reworks: ${if (inspection.sourceReworkIds.isEmpty()) "None" else inspection.sourceReworkIds.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Source Re-QC Cycles: ${if (inspection.sourceReQcIds.isEmpty()) "None" else inspection.sourceReQcIds.joinToString()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun FinalQcAuditCard(
    events: List<FinalQcActivityEvent>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Audit Trail",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (events.isEmpty()) {
                Text(text = "No activity recorded.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    events.forEach { event ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "• ${event.activityType.defaultLabel}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = event.timestamp,
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        if (!event.notes.isNullOrBlank()) {
                            Text(
                                text = "  ${event.notes}",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinalQcPassDialog(
    totalQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var acceptedQty by remember { mutableStateOf(totalQuantity.toString()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pass Final QC") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Confirm that all items meet production quality standards.")
                OutlinedTextField(
                    value = acceptedQty,
                    onValueChange = { acceptedQty = it },
                    label = { Text("Accepted Quantity") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Inspection Notes (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = acceptedQty.toIntOrNull() ?: totalQuantity
                    onConfirm(qty, notes.ifBlank { null })
                }
            ) {
                Text("Submit PASS")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinalQcFailDialog(
    totalQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String?) -> Unit
) {
    var rejectedQty by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Final QC Failure") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Record the reason for quality failure and rejected quantity.")
                OutlinedTextField(
                    value = rejectedQty,
                    onValueChange = { rejectedQty = it },
                    label = { Text("Rejected Quantity") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Failure Reason *") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Additional Notes") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = rejectedQty.toIntOrNull() ?: 1
                    onConfirm(qty, reason, notes.ifBlank { null })
                },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("Record FAIL")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FinalQcReleaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Authorize Production Release") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You are formally authorizing the release of this job from Quality Control. This action is immutable.")
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Release Notes (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes.ifBlank { null }) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
            ) {
                Text("Authorize Release")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
