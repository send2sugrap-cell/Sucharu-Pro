package com.sucharu.sucharupro.ui.features.production.job.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.job.ProductionActivityEvent
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStage
import com.sucharu.sucharupro.domain.model.job.ProductionStageExecution
import com.sucharu.sucharupro.domain.model.job.ProductionStageOutput
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.production.job.details.components.JobCancellationDialog
import com.sucharu.sucharupro.ui.features.production.job.details.components.JobHoldDialog
import com.sucharu.sucharupro.ui.features.production.job.details.components.JobLifecycleControlsCard
import com.sucharu.sucharupro.ui.features.production.job.details.components.ProductionActivityTimeline
import com.sucharu.sucharupro.ui.features.production.job.details.components.ProductionStageTimelineCard
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageCompletionDialog
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageExecutionWorkspaceCard
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageOperatorAssignmentDialog
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageOutputHistoryCard
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageOutputRecordDialog
import com.sucharu.sucharupro.ui.features.production.job.details.components.StageStartDialog
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Screen presenting Production Job Card details, execution workspace, output quantity tracking,
 * stage progression, operator assignment, and chronological activity timeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionJobDetailsScreen(
    jobId: String,
    viewModel: ProductionJobDetailsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCancelDialog by remember { mutableStateOf(false) }
    var showHoldDialog by remember { mutableStateOf(false) }
    var stageToStart by remember { mutableStateOf<ProductionJobStage?>(null) }
    var stageToCompleteId by remember { mutableStateOf<String?>(null) }
    var stageToAssign by remember { mutableStateOf<ProductionJobStage?>(null) }
    var stageToRecordOutput by remember { mutableStateOf<ProductionJobStage?>(null) }
    var isReassignmentDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    LaunchedEffect(uiState) {
        val success = uiState as? ProductionJobDetailsUiState.Success
        if (success?.actionMessage != null) {
            snackbarHostState.showSnackbar(success.actionMessage)
            viewModel.dismissActionFeedback()
        } else if (success?.actionError != null) {
            snackbarHostState.showSnackbar("Error: ${success.actionError}")
            viewModel.dismissActionFeedback()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState as? ProductionJobDetailsUiState.Success)?.job?.jobNumber ?: "Job Details"
                    Text(text = title)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProductionJobDetailsUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is ProductionJobDetailsUiState.NotFound -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Job Card Not Found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = "No production job found with ID: ${state.jobId}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                            AppButton(
                                text = "Go Back",
                                onClick = onNavigateBack
                            )
                        }
                    }
                }

                is ProductionJobDetailsUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(MaterialTheme.spacing.large),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Error Loading Job Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                            if (state.canRetry) {
                                AppButton(
                                    text = "Retry",
                                    onClick = { viewModel.loadJob(jobId) }
                                )
                            }
                        }
                    }
                }

                is ProductionJobDetailsUiState.Success -> {
                    val job = state.job
                    val activeStage = job.currentStage
                    val activeExecution = state.stageExecutions.find { it.stageId == activeStage?.stageId }
                    val activeStageOutputs = state.stageOutputs.filter { it.stageId == activeStage?.stageId }
                    val totalProducedForActiveStage = activeStageOutputs.sumOf { it.quantity }
                    val remainingForActiveStage = (job.quantity - totalProducedForActiveStage).coerceAtLeast(0)

                    JobDetailsContent(
                        job = job,
                        currentStage = activeStage,
                        currentExecution = activeExecution,
                        totalOutputQuantity = totalProducedForActiveStage,
                        remainingQuantity = remainingForActiveStage,
                        outputs = state.stageOutputs,
                        reconciliation = state.reconciliation,
                        checklist = state.completionChecklist,
                        activities = state.activities,
                        isActionInProgress = state.isActionInProgress,
                        onHoldClick = { showHoldDialog = true },
                        onResumeClick = { viewModel.resumeJob() },
                        onCancelClick = { showCancelDialog = true },
                        onMarkReadyClick = { viewModel.markJobReady() },
                        onConfirmCompletionClick = { showCompletionDialog = true },
                        onDeliverClick = { viewModel.deliverJob() },
                        onStartStageClick = { stageId ->
                            val stage = job.stages.find { it.stageId == stageId }
                            stageToStart = stage
                        },
                        onCompleteStageClick = { stageId -> stageToCompleteId = stageId },
                        onSkipStageClick = { stageId -> viewModel.skipStage(stageId) },
                        onRecordOutputClick = { stageId ->
                            val stage = job.stages.find { it.stageId == stageId }
                            stageToRecordOutput = stage
                        },
                        onAssignOperatorClick = { stage ->
                            stageToAssign = stage
                            isReassignmentDialog = false
                        },
                        onReassignOperatorClick = { stage ->
                            stageToAssign = stage
                            isReassignmentDialog = true
                        },
                        onUnassignOperatorClick = { stage ->
                            viewModel.unassignStageOperator(stage.stageId)
                        }
                    )

                    // Dialogs
                    if (showCompletionDialog) {
                        com.sucharu.sucharupro.ui.features.production.job.details.components.ProductionCompletionConfirmationDialog(
                            job = job,
                            checklist = state.completionChecklist,
                            onConfirm = { remarks ->
                                showCompletionDialog = false
                                viewModel.confirmProductionCompletion(remarks = remarks)
                            },
                            onDismiss = { showCompletionDialog = false }
                        )
                    }

                    if (showCancelDialog) {
                        JobCancellationDialog(
                            jobNumber = job.jobNumber,
                            onConfirm = { reason ->
                                showCancelDialog = false
                                viewModel.cancelJob(reason)
                            },
                            onDismiss = { showCancelDialog = false }
                        )
                    }

                    if (showHoldDialog) {
                        JobHoldDialog(
                            jobNumber = job.jobNumber,
                            onConfirm = { reason ->
                                showHoldDialog = false
                                viewModel.holdJob(reason)
                            },
                            onDismiss = { showHoldDialog = false }
                        )
                    }

                    stageToStart?.let { targetStage ->
                        StageStartDialog(
                            stage = targetStage,
                            jobNumber = job.jobNumber,
                            onConfirm = { startRemarks ->
                                viewModel.startStage(targetStage.stageId, notes = startRemarks)
                                stageToStart = null
                            },
                            onDismiss = { stageToStart = null }
                        )
                    }

                    stageToRecordOutput?.let { targetStage ->
                        val targetOutputs = state.stageOutputs.filter { it.stageId == targetStage.stageId }
                        val produced = targetOutputs.sumOf { it.quantity }
                        val remaining = (job.quantity - produced).coerceAtLeast(0)

                        StageOutputRecordDialog(
                            stage = targetStage,
                            plannedQuantity = job.quantity,
                            producedQuantity = produced,
                            remainingQuantity = remaining,
                            unit = job.unit,
                            onConfirm = { quantity, remarks ->
                                viewModel.recordStageOutput(
                                    stageId = targetStage.stageId,
                                    quantity = quantity,
                                    unit = job.unit,
                                    remarks = remarks
                                )
                                stageToRecordOutput = null
                            },
                            onDismiss = { stageToRecordOutput = null }
                        )
                    }

                    if (stageToCompleteId != null) {
                        val targetStageName = job.stages.find { it.stageId == stageToCompleteId }?.stageType?.defaultLabel ?: "Stage"
                        StageCompletionDialog(
                            stageName = targetStageName,
                            onConfirm = { notes ->
                                val stageId = stageToCompleteId ?: return@StageCompletionDialog
                                stageToCompleteId = null
                                viewModel.completeStage(stageId, notes = notes)
                            },
                            onDismiss = { stageToCompleteId = null }
                        )
                    }

                    stageToAssign?.let { targetStage ->
                        StageOperatorAssignmentDialog(
                            stage = targetStage,
                            availableOperators = viewModel.getAvailableOperators(),
                            isReassignment = isReassignmentDialog,
                            onConfirm = { operatorId, operatorName, notes ->
                                if (isReassignmentDialog) {
                                    viewModel.reassignStageOperator(
                                        stageId = targetStage.stageId,
                                        newOperatorId = operatorId,
                                        newOperatorName = operatorName,
                                        notes = notes
                                    )
                                } else {
                                    viewModel.assignStageOperator(
                                        stageId = targetStage.stageId,
                                        operatorId = operatorId,
                                        operatorName = operatorName,
                                        notes = notes
                                    )
                                }
                                stageToAssign = null
                            },
                            onDismiss = { stageToAssign = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobDetailsContent(
    job: ProductionJob,
    currentStage: ProductionJobStage?,
    currentExecution: ProductionStageExecution?,
    totalOutputQuantity: Int,
    remainingQuantity: Int,
    outputs: List<ProductionStageOutput>,
    reconciliation: com.sucharu.sucharupro.domain.model.job.ProductionOutputReconciliation? = null,
    checklist: com.sucharu.sucharupro.domain.model.job.ProductionCompletionChecklist? = null,
    activities: List<ProductionActivityEvent>,
    isActionInProgress: Boolean,
    onHoldClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onMarkReadyClick: () -> Unit,
    onConfirmCompletionClick: () -> Unit,
    onDeliverClick: () -> Unit,
    onStartStageClick: (stageId: String) -> Unit,
    onCompleteStageClick: (stageId: String) -> Unit,
    onSkipStageClick: (stageId: String) -> Unit,
    onRecordOutputClick: (stageId: String) -> Unit,
    onAssignOperatorClick: (stage: ProductionJobStage) -> Unit,
    onReassignOperatorClick: (stage: ProductionJobStage) -> Unit,
    onUnassignOperatorClick: (stage: ProductionJobStage) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        // Job Summary Card
        JobHeaderSummaryCard(job = job)

        // Stage Execution Workspace Card
        StageExecutionWorkspaceCard(
            job = job,
            currentStage = currentStage,
            currentExecution = currentExecution,
            totalOutputQuantity = totalOutputQuantity,
            remainingQuantity = remainingQuantity,
            isActionInProgress = isActionInProgress,
            onStartStageClick = onStartStageClick,
            onCompleteStageClick = onCompleteStageClick,
            onRecordOutputClick = onRecordOutputClick
        )

        // Production Output Summary & Quantity Reconciliation Card
        reconciliation?.let {
            com.sucharu.sucharupro.ui.features.production.job.details.components.ProductionOutputSummaryCard(reconciliation = it)
        }

        // Production Completion & Readiness Gate Card
        com.sucharu.sucharupro.ui.features.production.job.details.components.ProductionCompletionCard(
            job = job,
            checklist = checklist,
            isActionInProgress = isActionInProgress,
            onConfirmCompletionClick = onConfirmCompletionClick
        )

        // Stage Output History Card
        StageOutputHistoryCard(outputs = outputs)

        // Lifecycle Controls Card
        JobLifecycleControlsCard(
            job = job,
            isActionInProgress = isActionInProgress,
            onHoldClick = onHoldClick,
            onResumeClick = onResumeClick,
            onCancelClick = onCancelClick,
            onMarkReadyClick = onMarkReadyClick,
            onDeliverClick = onDeliverClick
        )

        // 13-Stage Production Timeline Card
        ProductionStageTimelineCard(
            job = job,
            isActionInProgress = isActionInProgress,
            onStartStageClick = onStartStageClick,
            onCompleteStageClick = onCompleteStageClick,
            onSkipStageClick = onSkipStageClick,
            onAssignOperatorClick = onAssignOperatorClick,
            onReassignOperatorClick = onReassignOperatorClick,
            onUnassignOperatorClick = onUnassignOperatorClick
        )

        // Production Activity Timeline Card
        ProductionActivityTimeline(activities = activities)
    }
}

@Composable
private fun JobHeaderSummaryCard(
    job: ProductionJob,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Job Card Specification",
        icon = Icons.Default.Info,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            Text(
                text = job.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val desc = job.description
            if (desc != null) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Order: ${job.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Planned Qty: ${job.quantity} ${job.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (job.specification != null) {
                Text(
                    text = "Spec: ${job.specification}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (job.notes != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text = "Notes / Remarks: ${job.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
