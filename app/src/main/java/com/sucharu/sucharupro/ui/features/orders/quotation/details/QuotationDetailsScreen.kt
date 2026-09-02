package com.sucharu.sucharupro.ui.features.orders.quotation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.features.orders.components.CommercialActivityTimeline
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.orders.quotation.components.QuotationStatusBadge
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationCommercialSummaryCard
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationDeliveryRequirementCard
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationItemsSection
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationPaymentTermsCard
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationRevisionHistorySection
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Commercial Quotation Details / Profile Screen with Revision History.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationDetailsScreen(
    viewModel: QuotationDetailsViewModel,
    onBackClick: () -> Unit,
    onEditClick: (String) -> Unit = {},
    onNavigateToOrder: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showSendDialog by remember { mutableStateOf(false) }
    var showNegotiationDialog by remember { mutableStateOf(false) }
    var showApprovalDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRevisionDialog by remember { mutableStateOf(false) }
    var showOrderConversionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    val currentState = uiState
    androidx.compose.runtime.LaunchedEffect(currentState) {
        if (currentState is QuotationDetailsUiState.Success) {
            currentState.actionMessage?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearActionMessage()
            }
            currentState.actionError?.let {
                snackbarHostState.showSnackbar("Error: $it")
                viewModel.clearActionError()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quotation Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to quotations"
                        )
                    }
                },
                actions = {
                    if (currentState is QuotationDetailsUiState.Success && currentState.quotation.status == com.sucharu.sucharupro.domain.model.order.QuotationStatusType.DRAFT) {
                        IconButton(onClick = { onEditClick(currentState.quotation.quotationId) }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                contentDescription = "Edit Draft Quotation"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is QuotationDetailsUiState.Loading -> {
                    QuotationDetailsLoadingView(modifier = Modifier.fillMaxSize())
                }

                is QuotationDetailsUiState.NotFound -> {
                    QuotationNotFoundView(
                        quotationId = state.quotationId,
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is QuotationDetailsUiState.Error -> {
                    QuotationDetailsErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is QuotationDetailsUiState.Success -> {
                    val quotation = state.quotation
                    val activeRev = state.activeRevision ?: quotation.currentRevision

                    QuotationDetailsContent(
                        state = state,
                        activityRepository = viewModel.activityRepository,
                        onSelectRevision = { revisionId -> viewModel.selectRevision(revisionId) },
                        onSendClick = { showSendDialog = true },
                        onStartNegotiationClick = { showNegotiationDialog = true },
                        onCreateRevisionClick = { showRevisionDialog = true },
                        onApproveClick = { showApprovalDialog = true },
                        onRejectClick = { showRejectDialog = true },
                        onCancelClick = { showCancelDialog = true },
                        onConvertToOrderClick = { showOrderConversionDialog = true },
                        onViewOrderClick = { orderId -> onNavigateToOrder(orderId) },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Lifecycle Dialogs
                    if (showSendDialog) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationSendConfirmDialog(
                            quotation = quotation,
                            onDismiss = { showSendDialog = false },
                            onConfirm = {
                                showSendDialog = false
                                viewModel.sendQuotation()
                            }
                        )
                    }

                    if (showNegotiationDialog) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationStartNegotiationDialog(
                            quotation = quotation,
                            onDismiss = { showNegotiationDialog = false },
                            onConfirm = {
                                showNegotiationDialog = false
                                viewModel.startNegotiation()
                            }
                        )
                    }

                    if (showApprovalDialog && activeRev != null) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationApprovalDialog(
                            quotation = quotation,
                            revision = activeRev,
                            onDismiss = { showApprovalDialog = false },
                            onConfirm = { approvedBy, notes ->
                                showApprovalDialog = false
                                viewModel.approveQuotation(activeRev.revisionId, approvedBy, notes)
                            }
                        )
                    }

                    if (showRejectDialog) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationRejectDialog(
                            quotation = quotation,
                            onDismiss = { showRejectDialog = false },
                            onConfirm = { reason ->
                                showRejectDialog = false
                                viewModel.rejectQuotation(reason)
                            }
                        )
                    }

                    if (showCancelDialog) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationCancelDialog(
                            quotation = quotation,
                            onDismiss = { showCancelDialog = false },
                            onConfirm = { reason ->
                                showCancelDialog = false
                                viewModel.cancelQuotation(reason)
                            }
                        )
                    }

                    if (showRevisionDialog && activeRev != null) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationRevisionDialog(
                            quotation = quotation,
                            baseRevision = activeRev,
                            onDismiss = { showRevisionDialog = false },
                            onConfirm = { newRevision ->
                                showRevisionDialog = false
                                viewModel.createRevision(newRevision)
                            }
                        )
                    }

                    if (showOrderConversionDialog && activeRev != null) {
                        com.sucharu.sucharupro.ui.features.orders.quotation.details.components.OrderConversionDialog(
                            quotation = quotation,
                            approvedRevision = quotation.revisions.find { it.revisionId == quotation.approvedRevisionId } ?: activeRev,
                            onDismiss = { showOrderConversionDialog = false },
                            onConfirm = { priority, confirmedBy ->
                                showOrderConversionDialog = false
                                viewModel.convertQuotationToOrder(
                                    priority = priority,
                                    confirmedBy = confirmedBy,
                                    onSuccess = { orderId -> onNavigateToOrder(orderId) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotationDetailsContent(
    state: QuotationDetailsUiState.Success,
    activityRepository: CommercialActivityRepository,
    onSelectRevision: (String?) -> Unit,
    onSendClick: () -> Unit,
    onStartNegotiationClick: () -> Unit,
    onCreateRevisionClick: () -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onCancelClick: () -> Unit,
    onConvertToOrderClick: () -> Unit,
    onViewOrderClick: (orderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quotation = state.quotation
    val activeRevision = state.activeRevision
    val activeItems = activeRevision?.items ?: quotation.items

    BoxWithConstraints(modifier = modifier) {
        val isTabletOrDesktop = maxWidth >= 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            if (isTabletOrDesktop) {
                // Tablet/Desktop: 2-column top section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    QuotationHeaderCard(
                        quotation = quotation,
                        modifier = Modifier.weight(1f)
                    )
                    QuotationReferencesCard(
                        quotation = quotation,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Phone: Stacked
                QuotationHeaderCard(quotation = quotation)
                QuotationReferencesCard(quotation = quotation)
            }

            // Lifecycle & Actions Section
            com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationLifecycleActionsCard(
                quotation = quotation,
                activeRevision = activeRevision,
                linkedOrders = state.linkedOrders,
                isActionInProgress = state.isActionInProgress,
                onSendClick = onSendClick,
                onStartNegotiationClick = onStartNegotiationClick,
                onCreateRevisionClick = onCreateRevisionClick,
                onApproveClick = onApproveClick,
                onRejectClick = onRejectClick,
                onCancelClick = onCancelClick,
                onConvertToOrderClick = onConvertToOrderClick,
                onViewOrderClick = onViewOrderClick
            )

            // Commercial Summary
            QuotationCommercialSummaryCard(
                quotation = quotation,
                activeRevision = activeRevision
            )

            // Items Section
            DetailSectionCard(
                title = "Quotation Items (${activeItems.size} line items)",
                icon = Icons.Default.Description
            ) {
                QuotationItemsSection(items = activeItems)
            }

            // Revision History Section
            QuotationRevisionHistorySection(
                quotation = quotation,
                revisions = state.revisions,
                selectedRevisionId = state.selectedRevisionId,
                onSelectRevision = onSelectRevision
            )

            // Payment and Delivery terms
            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    QuotationPaymentTermsCard(
                        paymentTerms = quotation.paymentTerms,
                        modifier = Modifier.weight(1f)
                    )
                    QuotationDeliveryRequirementCard(
                        deliveryRequirement = quotation.deliveryRequirement,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                QuotationPaymentTermsCard(paymentTerms = quotation.paymentTerms)
                QuotationDeliveryRequirementCard(deliveryRequirement = quotation.deliveryRequirement)
            }

            // Terms & Conditions if present
            val terms = quotation.termsAndConditions
            if (!terms.isNullOrBlank()) {
                DetailSectionCard(
                    title = "Terms & Conditions",
                    icon = Icons.Default.Gavel
                ) {
                    Text(
                        text = terms,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Activity Timeline ──
            DetailSectionCard(
                title = "Activity History",
                icon = Icons.Default.History
            ) {
                CommercialActivityTimeline(
                    activityRepository = activityRepository,
                    entityType = CommercialEntityType.QUOTATION,
                    entityId = quotation.quotationId
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@Composable
private fun QuotationHeaderCard(
    quotation: Quotation,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = quotation.quotationNumber,
        icon = Icons.Default.RequestQuote,
        trailingContent = {
            QuotationStatusBadge(status = quotation.status)
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Quotation ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quotation.quotationId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Created Date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quotation.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            val validUntil = quotation.validUntil
            if (!validUntil.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Valid Until",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = validUntil.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (quotation.status == com.sucharu.sucharupro.domain.model.order.QuotationStatusType.EXPIRED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (quotation.isApproved && !quotation.approvedBy.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Approved By",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${quotation.approvedBy} (${quotation.approvedAt?.take(10) ?: ""})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotationReferencesCard(
    quotation: Quotation,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "References",
        icon = Icons.Default.Link,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Customer ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = quotation.customerId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            val inqId = quotation.inquiryId
            if (!inqId.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Inquiry Reference",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inqId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Current Revision",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Rev #${quotation.currentRevisionNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuotationDetailsLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading quotation details...",
            size = 48.dp
        )
    }
}

@Composable
private fun QuotationNotFoundView(
    quotationId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.xxLarge)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "Quotation Not Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "No commercial quotation exists with ID: $quotationId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                AppButton(
                    text = "Back to Quotations",
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun QuotationDetailsErrorView(
    errorMessage: String,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(MaterialTheme.spacing.large)
        ) {
            Text(
                text = "Failed to load quotation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                AppOutlinedButton(
                    text = "Back",
                    onClick = onBackClick,
                    modifier = Modifier.weight(1f)
                )
                AppButton(
                    text = "Retry",
                    onClick = onRetry,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
