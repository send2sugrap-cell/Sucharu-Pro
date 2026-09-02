package com.sucharu.sucharupro.ui.features.orders.quotation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.domain.repository.OrderRepository
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel managing presentation state, historical revision inspection, commercial lifecycle
 * mutations, and Order conversion for Commercial Quotations.
 *
 * Integrates with [CommercialActivityRepository] to record audit events ONLY
 * after the underlying business operation succeeds.
 * A failed operation produces zero corresponding success audit events.
 */
class QuotationDetailsViewModel(
    private val quotationId: String,
    private val repository: QuotationRepository = QuotationRepositoryImpl(FakeQuotationDataSource()),
    private val orderRepository: OrderRepository = OrderRepositoryImpl(FakeOrderDataSource(), FakeQuotationDataSource()),
    val activityRepository: CommercialActivityRepository = CommercialActivityRepositoryImpl(
        FakeCommercialActivityDataSource()
    ),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<QuotationDetailsUiState>(QuotationDetailsUiState.Loading)
    val uiState: StateFlow<QuotationDetailsUiState> = _uiState.asStateFlow()

    private val selectedRevisionIdFlow = MutableStateFlow<String?>(null)
    private val actionInProgressFlow = MutableStateFlow(false)
    private val actionErrorFlow = MutableStateFlow<String?>(null)
    private val actionMessageFlow = MutableStateFlow<String?>(null)

    /** Guards against recording VIEWED more than once per screen entry. */
    private var viewedEventRecorded = false

    init {
        loadQuotation()
    }

    /** Observes the quotation, revision stream, and linked orders. */
    fun loadQuotation() {
        scope.launch {
            val quotationDataFlow = combine(
                repository.getQuotationById(quotationId),
                repository.getQuotationRevisions(quotationId),
                orderRepository.getOrdersForQuotation(quotationId)
            ) { quotation, revisions, linkedOrders ->
                Triple(quotation, revisions, linkedOrders)
            }

            combine(
                quotationDataFlow,
                selectedRevisionIdFlow,
                actionInProgressFlow,
                actionErrorFlow,
                actionMessageFlow
            ) { (quotation, revisions, linkedOrders), selectedRevId, inProgress, error, message ->
                DataHolder(quotation, revisions, linkedOrders, selectedRevId, inProgress, error, message)
            }
                .onStart {
                    _uiState.value = QuotationDetailsUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = QuotationDetailsUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load quotation details."
                    )
                }
                .collect { data ->
                    if (data.quotation != null) {
                        _uiState.value = QuotationDetailsUiState.Success(
                            quotation = data.quotation,
                            revisions = data.revisions.ifEmpty { data.quotation.revisions },
                            selectedRevisionId = data.selectedRevId,
                            linkedOrders = data.linkedOrders,
                            isActionInProgress = data.inProgress,
                            actionError = data.error,
                            actionMessage = data.message
                        )
                    } else {
                        _uiState.value = QuotationDetailsUiState.NotFound(quotationId = quotationId)
                    }
                }
        }
    }

    /** Allows the user to select and inspect a historical revision snapshot. */
    fun selectRevision(revisionId: String?) {
        selectedRevisionIdFlow.value = revisionId
    }

    /**
     * Records a one-time VIEWED event when the screen is explicitly entered.
     * Subsequent calls within the same lifecycle instance are no-ops.
     */
    fun recordViewedOnce(actorId: String? = null, actorName: String? = null) {
        if (viewedEventRecorded) return
        viewedEventRecorded = true
        scope.launch {
            activityRepository.recordActivity(
                buildEvent(
                    activityType = CommercialActivityType.VIEWED,
                    actorId = actorId,
                    actorName = actorName
                )
            )
        }
    }

    /** Transitions quotation from DRAFT to SENT. */
    fun sendQuotation(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentQuotationStatus()
        performLifecycleAction(
            action = { repository.updateQuotationStatus(quotationId, QuotationStatusType.SENT) },
            successMessage = "Quotation marked as SENT to customer.",
            onSuccess = {
                recordStatusChanged(previousStatus, QuotationStatusType.SENT.defaultLabel, actorId, actorName)
                onSuccess()
            }
        )
    }

    /** Transitions quotation from SENT/REJECTED to NEGOTIATION. */
    fun startNegotiation(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentQuotationStatus()
        performLifecycleAction(
            action = { repository.updateQuotationStatus(quotationId, QuotationStatusType.NEGOTIATION) },
            successMessage = "Quotation moved to NEGOTIATION stage.",
            onSuccess = {
                recordStatusChanged(previousStatus, QuotationStatusType.NEGOTIATION.defaultLabel, actorId, actorName)
                onSuccess()
            }
        )
    }

    /** Formally approves a specific revision of the quotation. */
    fun approveQuotation(
        revisionId: String,
        approvedBy: String,
        notes: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val timestamp = Instant.now().toString()
        performLifecycleAction(
            action = {
                repository.approveQuotationRevision(
                    quotationId = quotationId,
                    revisionId = revisionId,
                    approvedBy = approvedBy,
                    timestamp = timestamp
                )
            },
            successMessage = "Quotation Revision approved successfully.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.APPROVED,
                            actorId = actorId,
                            actorName = actorName ?: approvedBy,
                            newStatus = QuotationStatusType.APPROVED.defaultLabel,
                            newValue = revisionId,
                            note = notes
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Rejects the quotation, preserving revision history. */
    fun rejectQuotation(
        reason: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentQuotationStatus()
        performLifecycleAction(
            action = { repository.updateQuotationStatus(quotationId, QuotationStatusType.REJECTED) },
            successMessage = "Quotation marked as REJECTED.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.REJECTED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = previousStatus,
                            newStatus = QuotationStatusType.REJECTED.defaultLabel,
                            reason = reason
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Cancels the quotation, preserving revision history. */
    fun cancelQuotation(
        reason: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentQuotationStatus()
        performLifecycleAction(
            action = { repository.updateQuotationStatus(quotationId, QuotationStatusType.CANCELLED) },
            successMessage = "Quotation CANCELLED.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.CANCELLED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = previousStatus,
                            newStatus = QuotationStatusType.CANCELLED.defaultLabel,
                            reason = reason
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Adds a new sequential revision to the quotation. */
    fun createRevision(
        newRevision: QuotationRevision,
        reason: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: (String) -> Unit = {}
    ) {
        val previousRevisionNumber = currentLatestRevisionNumber()

        scope.launch {
            actionInProgressFlow.value = true
            actionErrorFlow.value = null
            actionMessageFlow.value = null

            when (val result = repository.createQuotationRevision(quotationId, newRevision)) {
                is DomainResult.Success -> {
                    actionInProgressFlow.value = false
                    actionMessageFlow.value = "Revision #${newRevision.revisionNumber} created successfully."
                    selectedRevisionIdFlow.value = result.data.revisionId

                    // Record REVISED audit event after successful revision creation
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.REVISED,
                            actorId = actorId,
                            actorName = actorName,
                            previousValue = previousRevisionNumber?.toString(),
                            newValue = result.data.revisionNumber.toString(),
                            reason = reason
                        )
                    )
                    onSuccess(result.data.revisionId)
                }
                is DomainResult.Error -> {
                    actionInProgressFlow.value = false
                    actionErrorFlow.value = result.message
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    /** Converts the approved quotation revision into a confirmed Order. */
    fun convertQuotationToOrder(
        priority: OrderPriority = OrderPriority.NORMAL,
        confirmedBy: String,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: (orderId: String) -> Unit = {}
    ) {
        val currentState = _uiState.value
        if (currentState !is QuotationDetailsUiState.Success) return

        val quotation = currentState.quotation
        val approvedRevId = quotation.approvedRevisionId

        if (!quotation.isApproved || approvedRevId.isNullOrBlank()) {
            actionErrorFlow.value = "Only APPROVED quotations can be converted into confirmed orders."
            return
        }

        val orderId = "ord-${UUID.randomUUID().toString().take(8)}"
        val orderNumber = "ORD-2026-${(1000..9999).random()}"
        val timestamp = Instant.now().toString()

        scope.launch {
            actionInProgressFlow.value = true
            actionErrorFlow.value = null
            actionMessageFlow.value = null

            when (val result = orderRepository.createOrderFromApprovedQuotation(
                orderId = orderId,
                orderNumber = orderNumber,
                quotationId = quotation.quotationId,
                approvedRevisionId = approvedRevId,
                priority = priority,
                confirmedBy = confirmedBy,
                timestamp = timestamp
            )) {
                is DomainResult.Success -> {
                    actionInProgressFlow.value = false
                    actionMessageFlow.value = "Order ${result.data.orderNumber} successfully created."

                    val createdOrderId = result.data.orderId

                    // Quotation receives ORDER_CONVERTED event with new order ID
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.ORDER_CONVERTED,
                            actorId = actorId,
                            actorName = actorName ?: confirmedBy,
                            newValue = createdOrderId,
                            note = "Converted to Order: ${result.data.orderNumber}"
                        )
                    )

                    // Order receives its own CREATED event — no circular ref: only entityId stored
                    activityRepository.recordActivity(
                        CommercialActivityEvent(
                            activityId = UUID.randomUUID().toString(),
                            entityType = CommercialEntityType.ORDER,
                            entityId = createdOrderId,
                            activityType = CommercialActivityType.CREATED,
                            actorId = actorId,
                            actorName = actorName ?: confirmedBy,
                            timestamp = Instant.now().toString(),
                            previousValue = quotationId,
                            note = "Created from Quotation. Order: ${result.data.orderNumber}"
                        )
                    )

                    onSuccess(createdOrderId)
                }
                is DomainResult.Error -> {
                    actionInProgressFlow.value = false
                    actionErrorFlow.value = result.message
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    fun clearActionError() {
        actionErrorFlow.value = null
    }

    fun clearActionMessage() {
        actionMessageFlow.value = null
    }

    /** Retries fetching the quotation record. */
    fun retry() {
        loadQuotation()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun performLifecycleAction(
        action: suspend () -> DomainResult<*>,
        successMessage: String,
        onSuccess: () -> Unit
    ) {
        scope.launch {
            actionInProgressFlow.value = true
            actionErrorFlow.value = null
            actionMessageFlow.value = null

            when (val result = action()) {
                is DomainResult.Success -> {
                    actionInProgressFlow.value = false
                    actionMessageFlow.value = successMessage
                    onSuccess()
                }
                is DomainResult.Error -> {
                    actionInProgressFlow.value = false
                    actionErrorFlow.value = result.message
                }
                DomainResult.Loading -> Unit
            }
        }
    }

    private fun currentQuotationStatus(): String? =
        (_uiState.value as? QuotationDetailsUiState.Success)?.quotation?.status?.defaultLabel

    private fun currentLatestRevisionNumber(): Int? =
        (_uiState.value as? QuotationDetailsUiState.Success)
            ?.revisions?.maxByOrNull { it.revisionNumber }?.revisionNumber

    private fun recordStatusChanged(
        previousStatus: String?,
        newStatus: String,
        actorId: String?,
        actorName: String?
    ) {
        scope.launch {
            activityRepository.recordActivity(
                buildEvent(
                    activityType = CommercialActivityType.STATUS_CHANGED,
                    actorId = actorId,
                    actorName = actorName,
                    previousStatus = previousStatus,
                    newStatus = newStatus
                )
            )
        }
    }

    private fun buildEvent(
        activityType: CommercialActivityType,
        actorId: String? = null,
        actorName: String? = null,
        previousStatus: String? = null,
        newStatus: String? = null,
        previousValue: String? = null,
        newValue: String? = null,
        reason: String? = null,
        note: String? = null
    ) = CommercialActivityEvent(
        activityId = UUID.randomUUID().toString(),
        entityType = CommercialEntityType.QUOTATION,
        entityId = quotationId,
        activityType = activityType,
        actorId = actorId,
        actorName = actorName,
        timestamp = Instant.now().toString(),
        previousStatus = previousStatus,
        newStatus = newStatus,
        previousValue = previousValue,
        newValue = newValue,
        reason = reason,
        note = note
    )

    private data class DataHolder(
        val quotation: com.sucharu.sucharupro.domain.model.order.Quotation?,
        val revisions: List<QuotationRevision>,
        val linkedOrders: List<com.sucharu.sucharupro.domain.model.order.Order>,
        val selectedRevId: String?,
        val inProgress: Boolean,
        val error: String?,
        val message: String?
    )
}
