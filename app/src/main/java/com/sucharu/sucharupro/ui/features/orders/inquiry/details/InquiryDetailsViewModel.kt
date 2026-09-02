package com.sucharu.sucharupro.ui.features.orders.inquiry.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeCommercialActivityDataSource
import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.repository.CommercialActivityRepositoryImpl
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityEvent
import com.sucharu.sucharupro.domain.model.activity.CommercialActivityType
import com.sucharu.sucharupro.domain.model.activity.CommercialEntityType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.order.InquiryStatusType
import com.sucharu.sucharupro.domain.repository.CommercialActivityRepository
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel managing presentation state for Customer Inquiry Details screen.
 *
 * Integrates with [CommercialActivityRepository] to record audit events ONLY
 * after the underlying business operation succeeds.
 * A failed operation produces zero corresponding success audit events.
 */
class InquiryDetailsViewModel(
    private val inquiryId: String,
    private val repository: InquiryRepository = InquiryRepositoryImpl(FakeInquiryDataSource()),
    val activityRepository: CommercialActivityRepository = CommercialActivityRepositoryImpl(
        FakeCommercialActivityDataSource()
    ),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<InquiryDetailsUiState>(InquiryDetailsUiState.Loading)
    val uiState: StateFlow<InquiryDetailsUiState> = _uiState.asStateFlow()

    /**
     * Guards against recording VIEWED more than once per screen entry.
     * Resets to false when a new [inquiryId] is loaded.
     */
    private var viewedEventRecorded = false

    init {
        loadInquiry()
    }

    /** Observes the reactive inquiry stream by ID. */
    fun loadInquiry() {
        scope.launch {
            repository.getInquiryById(inquiryId)
                .onStart {
                    _uiState.value = InquiryDetailsUiState.Loading
                }
                .catch { exception ->
                    _uiState.value = InquiryDetailsUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load inquiry details."
                    )
                }
                .collect { inquiry ->
                    if (inquiry != null) {
                        _uiState.value = InquiryDetailsUiState.Success(inquiry = inquiry)
                    } else {
                        _uiState.value = InquiryDetailsUiState.NotFound(inquiryId = inquiryId)
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

    /** Updates the inquiry status and records a STATUS_CHANGED audit event on success. */
    fun updateInquiryStatus(
        newStatus: InquiryStatusType,
        previousStatus: InquiryStatusType,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        scope.launch {
            when (val result = repository.updateInquiryStatus(inquiryId, newStatus)) {
                is DomainResult.Success -> {
                    // Record audit only AFTER success
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.STATUS_CHANGED,
                            actorId = actorId,
                            actorName = actorName,
                            previousStatus = previousStatus.defaultLabel,
                            newStatus = newStatus.defaultLabel
                        )
                    )
                    onSuccess()
                }
                is DomainResult.Error -> onError(result.message)
                DomainResult.Loading -> Unit
            }
        }
    }

    /** Updates the inquiry notes and records a NOTES_UPDATED audit event on success. */
    fun updateInquiryNotes(
        notes: String?,
        actorId: String? = null,
        actorName: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentInquiry = (_uiState.value as? InquiryDetailsUiState.Success)?.inquiry
            ?: return

        scope.launch {
            val updatedInquiry = currentInquiry.copy(
                notes = notes,
                updatedAt = Instant.now().toString()
            )
            when (val result = repository.updateInquiry(updatedInquiry)) {
                is DomainResult.Success -> {
                    activityRepository.recordActivity(
                        buildEvent(
                            activityType = CommercialActivityType.NOTES_UPDATED,
                            actorId = actorId,
                            actorName = actorName,
                            note = "Remarks updated."
                        )
                    )
                    onSuccess()
                }
                is DomainResult.Error -> onError(result.message)
                DomainResult.Loading -> Unit
            }
        }
    }

    /** Retries fetching the inquiry record. */
    fun retry() {
        loadInquiry()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

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
        entityType = CommercialEntityType.INQUIRY,
        entityId = inquiryId,
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
}
