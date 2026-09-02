package com.sucharu.sucharupro.ui.features.orders.order.details

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.ui.components.AppButton
import com.sucharu.sucharupro.ui.components.AppCard
import com.sucharu.sucharupro.ui.components.AppOutlinedButton
import com.sucharu.sucharupro.ui.components.LoadingIndicator
import com.sucharu.sucharupro.ui.components.OrderStatusBadge
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.ui.features.orders.components.CommercialActivityTimeline
import com.sucharu.sucharupro.ui.features.orders.components.DetailSectionCard
import com.sucharu.sucharupro.ui.features.orders.order.components.JobHandoffBadge
import com.sucharu.sucharupro.ui.features.orders.order.components.OrderPriorityBadge
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderCommercialSnapshotCard
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderHandoffConfirmationDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderJobHandoffConfirmationDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderCancellationDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderHandoffSummaryCard
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderHoldResumeDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderNotesDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderOperationalActionsCard
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderPriorityDialog
import com.sucharu.sucharupro.ui.features.orders.order.details.components.OrderItemsSection
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationDeliveryRequirementCard
import com.sucharu.sucharupro.ui.features.orders.quotation.details.components.QuotationPaymentTermsCard
import com.sucharu.sucharupro.ui.theme.spacing

/**
 * Commercial Order Details / Profile Screen with immutable financial snapshot, operational controls, and handoff status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    viewModel: OrderDetailsViewModel,
    onBackClick: () -> Unit,
    onNavigateToQuotation: (String) -> Unit = {},
    onNavigateToCustomer: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var showHandoffDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showHoldDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    val currentState = uiState
    LaunchedEffect(currentState) {
        if (currentState is OrderDetailsUiState.Success) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Order Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to orders"
                        )
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
                is OrderDetailsUiState.Loading -> {
                    OrderDetailsLoadingView(modifier = Modifier.fillMaxSize())
                }

                is OrderDetailsUiState.NotFound -> {
                    OrderNotFoundView(
                        orderId = state.orderId,
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is OrderDetailsUiState.Error -> {
                    OrderDetailsErrorView(
                        errorMessage = state.errorMessage,
                        onRetry = { viewModel.retry() },
                        onBackClick = onBackClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is OrderDetailsUiState.Success -> {
                    val order = state.order
                    val handoff = state.handoff
                    var showCreateHandoffDialog by remember { mutableStateOf(false) }

                    OrderDetailsContent(
                        order = order,
                        handoff = handoff,
                        isActionInProgress = state.isActionInProgress,
                        activityRepository = viewModel.activityRepository,
                        onConfirmOrderClick = { viewModel.confirmOrder() },
                        onMarkReadyForJobClick = { showHandoffDialog = true },
                        onSetPriorityClick = { showPriorityDialog = true },
                        onPutOnHoldClick = { showHoldDialog = true },
                        onResumeOrderClick = { showResumeDialog = true },
                        onCancelOrderClick = { showCancelDialog = true },
                        onEditNotesClick = { showNotesDialog = true },
                        onInitiateHandoff = { showCreateHandoffDialog = true },
                        onConfirmHandoff = { handoffId -> viewModel.confirmHandoff(handoffId) },
                        onMarkReadyForProduction = { handoffId -> viewModel.markHandoffReadyForProduction(handoffId) },
                        onNavigateToCustomer = onNavigateToCustomer,
                        onNavigateToQuotation = onNavigateToQuotation
                    )

                    // ── Dialogs ──

                    if (showCreateHandoffDialog) {
                        OrderJobHandoffConfirmationDialog(
                            order = order,
                            onDismiss = { showCreateHandoffDialog = false },
                            onConfirm = { notes ->
                                showCreateHandoffDialog = false
                                viewModel.createHandoff(notes = notes)
                            }
                        )
                    }

                    if (showHandoffDialog) {
                        OrderHandoffConfirmationDialog(
                            order = order,
                            onDismiss = { showHandoffDialog = false },
                            onConfirm = {
                                showHandoffDialog = false
                                viewModel.markReadyForJob()
                            }
                        )
                    }

                    if (showCancelDialog) {
                        OrderCancellationDialog(
                            order = order,
                            onDismiss = { showCancelDialog = false },
                            onConfirm = { reason ->
                                showCancelDialog = false
                                viewModel.cancelOrder(reason)
                            }
                        )
                    }

                    if (showPriorityDialog) {
                        OrderPriorityDialog(
                            currentPriority = order.priority,
                            onDismiss = { showPriorityDialog = false },
                            onConfirm = { priority ->
                                showPriorityDialog = false
                                viewModel.setOrderPriority(priority)
                            }
                        )
                    }

                    if (showHoldDialog) {
                        OrderHoldResumeDialog(
                            order = order,
                            isPuttingOnHold = true,
                            onDismiss = { showHoldDialog = false },
                            onConfirm = {
                                showHoldDialog = false
                                viewModel.putOnHold()
                            }
                        )
                    }

                    if (showResumeDialog) {
                        OrderHoldResumeDialog(
                            order = order,
                            isPuttingOnHold = false,
                            onDismiss = { showResumeDialog = false },
                            onConfirm = {
                                showResumeDialog = false
                                viewModel.resumeOrder()
                            }
                        )
                    }

                    if (showNotesDialog) {
                        OrderNotesDialog(
                            currentNotes = order.notes,
                            onDismiss = { showNotesDialog = false },
                            onConfirm = { notes ->
                                showNotesDialog = false
                                viewModel.updateNotes(notes)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsContent(
    order: Order,
    handoff: com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff? = null,
    isActionInProgress: Boolean,
    activityRepository: CommercialActivityRepository,
    onConfirmOrderClick: () -> Unit,
    onMarkReadyForJobClick: () -> Unit,
    onSetPriorityClick: () -> Unit,
    onPutOnHoldClick: () -> Unit,
    onResumeOrderClick: () -> Unit,
    onCancelOrderClick: () -> Unit,
    onEditNotesClick: () -> Unit,
    onInitiateHandoff: () -> Unit = {},
    onConfirmHandoff: (String) -> Unit = {},
    onMarkReadyForProduction: (String) -> Unit = {},
    onNavigateToCustomer: (String) -> Unit = {},
    onNavigateToQuotation: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                    OrderHeaderCard(
                        order = order,
                        modifier = Modifier.weight(1f)
                    )
                    OrderReferencesCard(
                        order = order,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Phone: Stacked
                OrderHeaderCard(order = order)
                OrderReferencesCard(order = order)
            }

            // Operational Lifecycle Controls
            OrderOperationalActionsCard(
                order = order,
                isActionInProgress = isActionInProgress,
                onConfirmOrderClick = onConfirmOrderClick,
                onMarkReadyForJobClick = onMarkReadyForJobClick,
                onSetPriorityClick = onSetPriorityClick,
                onPutOnHoldClick = onPutOnHoldClick,
                onResumeOrderClick = onResumeOrderClick,
                onCancelOrderClick = onCancelOrderClick,
                onEditNotesClick = onEditNotesClick
            )

            // Commercial Snapshot Financials
            OrderCommercialSnapshotCard(order = order)

            // Items Section
            DetailSectionCard(
                title = "Order Items (${order.items.size} line items)",
                icon = Icons.Default.Description
            ) {
                OrderItemsSection(items = order.items)
            }

            // Payment and Delivery terms
            if (isTabletOrDesktop) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                ) {
                    QuotationPaymentTermsCard(
                        paymentTerms = order.paymentTerms,
                        modifier = Modifier.weight(1f)
                    )
                    QuotationDeliveryRequirementCard(
                        deliveryRequirement = order.deliveryRequirement,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                QuotationPaymentTermsCard(paymentTerms = order.paymentTerms)
                QuotationDeliveryRequirementCard(deliveryRequirement = order.deliveryRequirement)
            }

            // Operational Notes if present
            val notes = order.notes
            if (!notes.isNullOrBlank()) {
                DetailSectionCard(
                    title = "Order Remarks & Operational Notes",
                    icon = Icons.AutoMirrored.Filled.Notes
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // ── Handoff Summary Card ──
            OrderHandoffSummaryCard(
                order = order,
                handoff = handoff,
                onInitiateHandoff = onInitiateHandoff,
                onConfirmHandoff = onConfirmHandoff,
                onMarkReadyForProduction = onMarkReadyForProduction
            )

            // ── Activity Timeline ──
            DetailSectionCard(
                title = "Activity History",
                icon = Icons.Default.History
            ) {
                CommercialActivityTimeline(
                    activityRepository = activityRepository,
                    entityType = CommercialEntityType.ORDER,
                    entityId = order.orderId
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
        }
    }
}

@Composable
private fun OrderHeaderCard(
    order: Order,
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = order.orderNumber,
        icon = Icons.Default.ShoppingCart,
        trailingContent = {
            OrderStatusBadge(status = order.status)
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OrderPriorityBadge(priority = order.priority)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Job Handoff Readiness",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                JobHandoffBadge(status = order.jobHandoffStatus)
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
                    text = order.createdAt.take(10),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            val confirmedAt = order.confirmedAt
            if (!confirmedAt.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confirmed Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = confirmedAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderReferencesCard(
    order: Order,
    onCustomerClick: (String) -> Unit = {},
    onQuotationClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    DetailSectionCard(
        title = "Commercial References",
        icon = Icons.Default.Link,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCustomerClick(order.customerId) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customer ID",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.customerId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Customer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            val qId = order.quotationId
            if (!qId.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQuotationClick(qId) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quotation Reference",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = qId,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "View Quotation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val revId = order.approvedQuotationRevisionId
            if (!revId.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Approved Revision",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = revId,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val confBy = order.confirmedBy
            if (!confBy.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confirmed By",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = confBy,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(
            message = "Loading order details...",
            size = 48.dp
        )
    }
}

@Composable
private fun OrderNotFoundView(
    orderId: String,
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
                    text = "Order Not Found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "No commercial order exists with ID: $orderId",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                AppButton(
                    text = "Back to Orders",
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsErrorView(
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
                text = "Failed to load order",
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
