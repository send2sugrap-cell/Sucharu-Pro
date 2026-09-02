package com.sucharu.sucharupro.ui.features.customer.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.core.validation.CustomerValidation
import com.sucharu.sucharupro.data.repository.FakeCustomerRepository
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerActivity
import com.sucharu.sucharupro.domain.model.customer.CustomerNote
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel managing presentation state for the Customer Details / Profile screen.
 *
 * Coordinates profile data, internal customer notes, operational activities, and follow-up metadata.
 */
class CustomerDetailsViewModel(
    private val customerId: String,
    private val repository: CustomerRepository = FakeCustomerRepository(),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow<CustomerDetailsUiState>(CustomerDetailsUiState.Loading)
    val uiState: StateFlow<CustomerDetailsUiState> = _uiState.asStateFlow()

    init {
        loadCustomer()
    }

    /**
     * Loads and reactively observes the customer record, notes, and activities for [customerId].
     */
    fun loadCustomer() {
        scope.launch {
            combine(
                repository.getCustomerById(customerId),
                repository.observeCustomerNotes(customerId),
                repository.observeCustomerActivities(customerId)
            ) { customer, notes, activities ->
                Triple(customer, notes, activities)
            }
                .onStart {
                    if (_uiState.value !is CustomerDetailsUiState.Success) {
                        _uiState.value = CustomerDetailsUiState.Loading
                    }
                }
                .catch { exception ->
                    _uiState.value = CustomerDetailsUiState.Error(
                        errorMessage = exception.localizedMessage ?: "Failed to load customer profile."
                    )
                }
                .collect { (customer, notes, activities) ->
                    if (customer != null) {
                        val currentSuccess = _uiState.value as? CustomerDetailsUiState.Success
                        _uiState.value = CustomerDetailsUiState.Success(
                            customer = customer,
                            notes = notes,
                            activities = activities,
                            isNoteDialogVisible = currentSuccess?.isNoteDialogVisible ?: false,
                            noteInputText = currentSuccess?.noteInputText ?: "",
                            isNoteImportantInput = currentSuccess?.isNoteImportantInput ?: false,
                            editingNoteId = currentSuccess?.editingNoteId,
                            noteErrorMessage = currentSuccess?.noteErrorMessage,
                            isNoteSaving = currentSuccess?.isNoteSaving ?: false,
                            isFollowUpDialogVisible = currentSuccess?.isFollowUpDialogVisible ?: false,
                            followUpInput = currentSuccess?.followUpInput ?: (customer.nextFollowUpAt ?: "")
                        )
                    } else {
                        _uiState.value = CustomerDetailsUiState.NotFound(customerId = customerId)
                    }
                }
        }
    }

    /**
     * Retries loading the customer profile.
     */
    fun retry() {
        loadCustomer()
    }

    // ========================================================================
    // Customer Notes Management
    // ========================================================================

    fun onOpenAddNoteDialog() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isNoteDialogVisible = true,
            editingNoteId = null,
            noteInputText = "",
            isNoteImportantInput = false,
            noteErrorMessage = null
        )
    }

    fun onOpenEditNoteDialog(note: CustomerNote) {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isNoteDialogVisible = true,
            editingNoteId = note.id,
            noteInputText = note.text,
            isNoteImportantInput = note.isImportant,
            noteErrorMessage = null
        )
    }

    fun onDismissNoteDialog() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isNoteDialogVisible = false,
            editingNoteId = null,
            noteInputText = "",
            isNoteImportantInput = false,
            noteErrorMessage = null,
            isNoteSaving = false
        )
    }

    fun onNoteTextChanged(text: String) {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            noteInputText = text,
            noteErrorMessage = null
        )
    }

    fun onNoteImportanceChanged(isImportant: Boolean) {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(isNoteImportantInput = isImportant)
    }

    fun saveNote() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        val validationError = CustomerValidation.validateNoteText(current.noteInputText)
        if (validationError != null) {
            _uiState.value = current.copy(noteErrorMessage = validationError)
            return
        }

        val trimmedText = current.noteInputText.trim()
        val timestamp = "2026-08-15T12:00:00Z"

        scope.launch {
            _uiState.value = current.copy(isNoteSaving = true, noteErrorMessage = null)

            val result = if (current.editingNoteId == null) {
                // Add new note
                val newNote = CustomerNote(
                    id = "note-${UUID.randomUUID().toString().take(8)}",
                    customerId = customerId,
                    text = trimmedText,
                    isImportant = current.isNoteImportantInput,
                    authorName = "Operations Staff",
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                repository.addCustomerNote(newNote)
            } else {
                // Update existing note
                val existingNote = current.notes.find { it.id == current.editingNoteId }
                val updatedNote = CustomerNote(
                    id = current.editingNoteId,
                    customerId = customerId,
                    text = trimmedText,
                    isImportant = current.isNoteImportantInput,
                    authorName = existingNote?.authorName ?: "Operations Staff",
                    createdAt = existingNote?.createdAt ?: timestamp,
                    updatedAt = timestamp
                )
                repository.updateCustomerNote(updatedNote)
            }

            when (result) {
                is DomainResult.Success -> {
                    onDismissNoteDialog()
                }
                is DomainResult.Error -> {
                    val updatedState = _uiState.value as? CustomerDetailsUiState.Success
                    if (updatedState != null) {
                        _uiState.value = updatedState.copy(
                            isNoteSaving = false,
                            noteErrorMessage = result.message
                        )
                    }
                }
                is DomainResult.Loading -> Unit
            }
        }
    }

    fun deleteNote(noteId: String) {
        scope.launch {
            repository.deleteCustomerNote(noteId = noteId, customerId = customerId)
        }
    }

    fun toggleNoteImportance(noteId: String) {
        scope.launch {
            repository.toggleImportantNote(noteId = noteId, customerId = customerId)
        }
    }

    // ========================================================================
    // Follow-up Metadata Management
    // ========================================================================

    fun onOpenFollowUpDialog() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isFollowUpDialogVisible = true,
            followUpInput = current.customer.nextFollowUpAt ?: ""
        )
    }

    fun onDismissFollowUpDialog() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(isFollowUpDialogVisible = false)
    }

    fun onFollowUpInputChanged(dateText: String) {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(followUpInput = dateText)
    }

    fun saveFollowUp() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        val dateValue = current.followUpInput.trim().ifBlank { null }
        scope.launch {
            repository.setFollowUpDate(customerId = customerId, followUpAt = dateValue)
            onDismissFollowUpDialog()
        }
    }

    fun clearFollowUp() {
        scope.launch {
            repository.setFollowUpDate(customerId = customerId, followUpAt = null)
            onDismissFollowUpDialog()
        }
    }

    // ========================================================================
    // Lifecycle & Account Status Control
    // ========================================================================

    fun promptStatusChange(status: com.sucharu.sucharupro.domain.model.customer.CustomerStatusType) {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isStatusConfirmDialogVisible = true,
            pendingStatus = status
        )
    }

    fun dismissStatusConfirmDialog() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        _uiState.value = current.copy(
            isStatusConfirmDialogVisible = false,
            pendingStatus = null
        )
    }

    fun confirmStatusChange() {
        val current = _uiState.value as? CustomerDetailsUiState.Success ?: return
        val targetStatus = current.pendingStatus ?: return
        scope.launch {
            repository.setCustomerStatus(customerId = customerId, status = targetStatus)
            dismissStatusConfirmDialog()
        }
    }

    fun deactivateCustomer() {
        promptStatusChange(com.sucharu.sucharupro.domain.model.customer.CustomerStatusType.INACTIVE)
    }

    fun reactivateCustomer() {
        promptStatusChange(com.sucharu.sucharupro.domain.model.customer.CustomerStatusType.ACTIVE)
    }
}
