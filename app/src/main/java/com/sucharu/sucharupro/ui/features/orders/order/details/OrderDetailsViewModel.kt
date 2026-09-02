package com.sucharu.sucharupro.ui.features.orders.order.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.domain.repository.OrderRepository
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
 * ViewModel managing presentation state, operational lifecycle mutations, priority control,
 * and job handoff readiness for Commercial Orders.
 *
 * Integrates with [CommercialActivityRepository] to record audit events ONLY
 * after the underlying business operation succeeds.
 * A failed operation produces zero corresponding success audit events.
 */
class OrderDetailsViewModel(
    private val orderId: String,
    private val repository: OrderRepository = OrderRepositoryImpl(FakeOrderDataSource()),
    val activityRepository: CommercialActivityRepository = CommercialActivityRepositoryImpl(
        FakeCommercialActivityDataSource()
    ),
    val handoffRepository: com.sucharu.sucharupro.domain.repository.OrderJobHandoffRepository = com.sucharu.sucharupro.data.repository.OrderJobHandoffRepositoryImpl(
        com.sucharu.sucharupro.data.datasource.FakeOrderJobHandoffDataSource()
    ),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<OrderDetailsUiState>(OrderDetailsUiState.Loading)
    val uiState: StateFlow<OrderDetailsUiState> = _uiState.asStateFlow()

    private val actionInProgressFlow = MutableStateFlow(false)
    private val actionErrorFlow = MutableStateFlow<String?>(null)
    private val actionMessageFlow = MutableStateFlow<String?>(null)

    /** Guards against recording VIEWED more than once per screen entry. */
    private var viewedEventRecorded = false

    init {
        loadOrder()
    }

    /** Observes the reactive order stream by ID, handoff record, and action feedback. */
    fun loadOrder() {
        scope.launch {
            combine(
                repository.getOrderById(orderId),
                handoffRepository.getHandoffForOrder(orderId),
                actionInProgressFlow,
                actionErrorFlow,
                actionMessageFlow
            ) { order, handoff, inProgress, error, message ->
                DataHolder(order, handoff, inProgress, error, message)
            }
                .onStart {
                    _uiState.value = OrderDetailsUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = OrderDetailsUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load order details."
                    )
                }
                .collect { data ->
                    if (data.order != null) {
                        _uiState.value = OrderDetailsUiState.Success(
                            order = data.order,
                            handoff = data.handoff,
                            isActionInProgress = data.inProgress,
                            actionError = data.error,
                            actionMessage = data.message
                        )
                    } else {
                        _uiState.value = OrderDetailsUiState.NotFound(orderId = orderId)
                    }
                }
        }
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

    /** Updates the operational commercial priority of the order. */
    fun setOrderPriority(
        priority: OrderPriority,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousPriority = currentOrderPriority()
        performAction(
            action = { repository.updateOrderPriority(orderId, priority) },
            successMessage = "Order priority updated to ${priority.defaultLabel}.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.PRIORITY_CHANGED,
                            actorId = actorId,
                            actorName = actorName,
                            previousValue = previousPriority?.defaultLabel,
                            newValue = priority.defaultLabel
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Validates business rules and marks the order as READY FOR JOB handoff. */
    fun markReadyForJob(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        performAction(
            action = { repository.markReadyForJob(orderId) },
            successMessage = "Order marked as Ready for Job Handoff.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.HANDOFF_READY,
                            actorId = actorId,
                            actorName = actorName,
                            newValue = "READY_FOR_JOB",
                            note = "Commercial handoff readiness confirmed."
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Transitions order from PENDING to CONFIRMED. */
    fun confirmOrder(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentOrderStatus()
        performAction(
            action = { repository.updateOrderStatus(orderId, OrderStatusType.CONFIRMED) },
            successMessage = "Order confirmed successfully.",
            onSuccess = {
                recordStatusChanged(previousStatus, OrderStatusType.CONFIRMED.defaultLabel, actorId, actorName)
                onSuccess()
            }
        )
    }

    /** Transitions order to ON HOLD. */
    fun putOnHold(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentOrderStatus()
        performAction(
            action = { repository.updateOrderStatus(orderId, OrderStatusType.ON_HOLD) },
            successMessage = "Order placed ON HOLD.",
            onSuccess = {
                recordStatusChanged(previousStatus, OrderStatusType.ON_HOLD.defaultLabel, actorId, actorName)
                onSuccess()
            }
        )
    }

    /** Resumes order from ON HOLD to CONFIRMED. */
    fun resumeOrder(
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val previousStatus = currentOrderStatus()
        performAction(
            action = { repository.updateOrderStatus(orderId, OrderStatusType.CONFIRMED) },
            successMessage = "Order resumed to CONFIRMED status.",
            onSuccess = {
                recordStatusChanged(previousStatus, OrderStatusType.CONFIRMED.defaultLabel, actorId, actorName)
                onSuccess()
            }
        )
    }

    /** Cancels the order with a mandatory reason. */
    fun cancelOrder(
        reason: String,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        if (reason.isBlank()) {
            actionErrorFlow.value = "Cancellation reason is required."
            return
        }
        val previousStatus = currentOrderStatus()
        performAction(
            action = { repository.cancelOrder(orderId, reason) },
            successMessage = "Order CANCELLED.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.CANCELLED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = previousStatus,
                            newStatus = OrderStatusType.CANCELLED.defaultLabel,
                            reason = reason
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Updates operational remarks/notes on the order. */
    fun updateNotes(
        notes: String?,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        performAction(
            action = { repository.updateOrderNotes(orderId, notes) },
            successMessage = "Remarks updated successfully.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.NOTES_UPDATED,
                            actorId = actorId,
                            actorName = actorName,
                            note = "Operational remarks updated."
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    fun clearActionMessage() {
        actionMessageFlow.value = null
    }

    fun clearActionError() {
        actionErrorFlow.value = null
    }

    /** Retries fetching the order record. */
    fun retry() {
        loadOrder()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun performAction(
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

    private fun currentOrderStatus(): String? =
        (_uiState.value as? OrderDetailsUiState.Success)?.order?.status?.defaultLabel

    private fun currentOrderPriority(): OrderPriority? =
        (_uiState.value as? OrderDetailsUiState.Success)?.order?.priority

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
        entityType = CommercialEntityType.ORDER,
        entityId = orderId,
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

    /** Creates a formal OrderJobHandoff snapshot for the current confirmed order. */
    fun createHandoff(
        notes: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val currentOrder = (_uiState.value as? OrderDetailsUiState.Success)?.order
        if (currentOrder == null) {
            actionErrorFlow.value = "Cannot create handoff: Order data not loaded."
            return
        }
        val handoffId = "hnd-${UUID.randomUUID().toString().take(8)}"
        val now = Instant.now().toString()

        performAction(
            action = {
                handoffRepository.createHandoff(
                    handoffId = handoffId,
                    order = currentOrder,
                    createdBy = actorName ?: "System",
                    notes = notes,
                    timestamp = now
                )
            },
            successMessage = "Order handoff created successfully.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.HANDOFF_READY,
                            actorId = actorId,
                            actorName = actorName,
                            newValue = "READY_FOR_HANDOFF",
                            note = "OrderJobHandoff snapshot '$handoffId' created."
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Confirms the handoff and transitions status to HANDED_OFF. */
    fun confirmHandoff(
        handoffId: String,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val now = Instant.now().toString()
        performAction(
            action = {
                handoffRepository.confirmHandoff(
                    handoffId = handoffId,
                    confirmedBy = actorName ?: "Production Desk",
                    timestamp = now
                )
            },
            successMessage = "Handoff to Production confirmed.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.STATUS_CHANGED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = "Ready for Handoff",
                            newStatus = "Handed Off",
                            note = "Handoff '$handoffId' confirmed to Production."
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    /** Marks the handoff record as READY_FOR_PRODUCTION. */
    fun markHandoffReadyForProduction(
        handoffId: String,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val now = Instant.now().toString()
        performAction(
            action = {
                handoffRepository.markReadyForProduction(
                    handoffId = handoffId,
                    timestamp = now
                )
            },
            successMessage = "Handoff is now READY FOR PRODUCTION.",
            onSuccess = {
                scope.launch {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.STATUS_CHANGED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = "Handed Off",
                            newStatus = "Ready for Production",
                            note = "Handoff '$handoffId' ready for production intake."
                        )
                    )
                }
                onSuccess()
            }
        )
    }

    private data class DataHolder(
        val order: com.sucharu.sucharupro.domain.model.order.Order?,
        val handoff: com.sucharu.sucharupro.domain.model.handoff.OrderJobHandoff?,
        val inProgress: Boolean,
        val error: String?,
        val message: String?
    )
}
