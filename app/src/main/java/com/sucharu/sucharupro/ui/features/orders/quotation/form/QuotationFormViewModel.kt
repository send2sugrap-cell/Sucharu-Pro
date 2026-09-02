package com.sucharu.sucharupro.ui.features.orders.quotation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInquiryDataSource
import com.sucharu.sucharupro.data.datasource.FakeQuotationDataSource
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.InquiryRepositoryImpl
import com.sucharu.sucharupro.data.repository.QuotationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.Quotation
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationRevision
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType
import com.sucharu.sucharupro.domain.repository.CustomerRepository
import com.sucharu.sucharupro.domain.repository.InquiryRepository
import com.sucharu.sucharupro.domain.repository.QuotationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel managing UI state, validation, financial calculation, and repository mutations for Quotation Create and Edit.
 */
class QuotationFormViewModel(
    private val quotationId: String? = null,
    initialInquiryId: String? = null,
    initialCustomerId: String? = null,
    private val quotationRepository: QuotationRepository = QuotationRepositoryImpl(FakeQuotationDataSource()),
    private val customerRepository: CustomerRepository = CustomerRepositoryImpl(FakeCustomerDataSource()),
    private val inquiryRepository: InquiryRepository = InquiryRepositoryImpl(FakeInquiryDataSource()),
    private val externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope
        get() = externalScope ?: viewModelScope

    private val formMode = if (quotationId.isNullOrBlank()) QuotationFormMode.CREATE else QuotationFormMode.EDIT_DRAFT

    private val _uiState = MutableStateFlow(
        QuotationFormUiState(
            mode = formMode,
            quotationId = quotationId,
            inquiryId = initialInquiryId.orEmpty(),
            customerId = initialCustomerId.orEmpty(),
            isLoading = formMode == QuotationFormMode.EDIT_DRAFT
        )
    )
    val uiState: StateFlow<QuotationFormUiState> = _uiState.asStateFlow()

    private var existingQuotation: Quotation? = null

    init {
        loadAvailableCustomers()
        if (formMode == QuotationFormMode.EDIT_DRAFT && !quotationId.isNullOrBlank()) {
            loadExistingQuotation(quotationId)
        } else if (!initialInquiryId.isNullOrBlank()) {
            loadInitialInquiry(initialInquiryId)
        }
    }

    private fun loadAvailableCustomers() {
        customerRepository.getCustomers()
            .onEach { customers ->
                _uiState.value = _uiState.value.copy(
                    availableCustomers = customers,
                    customerName = customers.find { it.customerId == _uiState.value.customerId }?.displayName
                )
            }
            .catch {
                // Silently ignore customer loading failure
            }
            .launchIn(scope)
    }

    private fun loadInitialInquiry(inqId: String) {
        scope.launch {
            inquiryRepository.getInquiryById(inqId)
                .catch { /* ignore */ }
                .collect { inquiry ->
                    if (inquiry != null) {
                        _uiState.value = _uiState.value.copy(
                            inquiryId = inquiry.inquiryId,
                            customerId = inquiry.customerId,
                            customerName = _uiState.value.availableCustomers.find { it.customerId == inquiry.customerId }?.displayName,
                            // Pre-fill quotation items from inquiry requirements if available
                            items = if (_uiState.value.items.isEmpty()) {
                                inquiry.items.map { req ->
                                    QuotationItem(
                                        itemId = "qitem-${UUID.randomUUID().toString().take(8)}",
                                        description = req.productName,
                                        specification = "${req.description}" +
                                            (if (!req.size.isNullOrBlank()) ", Size: ${req.size}" else "") +
                                            (if (!req.paperMaterial.isNullOrBlank()) ", Paper: ${req.paperMaterial}" else "") +
                                            (if (req.gsm != null) ", ${req.gsm} GSM" else ""),
                                        quantity = req.quantity,
                                        unit = req.unit,
                                        unitPrice = Money(0.0),
                                        discount = Money.ZERO,
                                        notes = req.notes
                                    )
                                }
                            } else {
                                _uiState.value.items
                            }
                        )
                    }
                }
        }
    }

    private fun loadExistingQuotation(id: String) {
        scope.launch {
            quotationRepository.getQuotationById(id)
                .catch { ex ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ex.localizedMessage ?: "Failed to load quotation."
                    )
                }
                .collect { quotation ->
                    if (quotation != null) {
                        existingQuotation = quotation
                        val isDraft = quotation.status == QuotationStatusType.DRAFT
                        val latestRevision = quotation.currentRevision

                        val matchedCustomer = _uiState.value.availableCustomers.find { it.customerId == quotation.customerId }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            quotationId = quotation.quotationId,
                            quotationNumber = quotation.quotationNumber,
                            customerId = quotation.customerId,
                            customerName = matchedCustomer?.displayName,
                            inquiryId = quotation.inquiryId.orEmpty(),
                            status = quotation.status,
                            items = latestRevision?.items ?: emptyList(),
                            quotationDiscountText = latestRevision?.discount?.amount?.stripTrailingZeros()?.toPlainString() ?: "0",
                            paymentTerms = latestRevision?.paymentTerms ?: PaymentTerms.DEFAULT,
                            deliveryRequirement = latestRevision?.deliveryRequirement,
                            validUntil = quotation.validUntil.orEmpty(),
                            termsAndConditions = quotation.termsAndConditions.orEmpty(),
                            notes = latestRevision?.notes.orEmpty(),
                            createdAt = quotation.createdAt,
                            isImmutableError = !isDraft,
                            errorMessage = if (!isDraft) {
                                "Only DRAFT quotations can be edited. This quotation is ${quotation.status.defaultLabel} and cannot be modified."
                            } else null
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Quotation not found."
                        )
                    }
                }
        }
    }

    fun onCustomerSelected(customer: Customer) {
        _uiState.value = _uiState.value.copy(
            customerId = customer.customerId,
            customerName = customer.displayName,
            customerIdError = null
        )
    }

    fun onInquiryIdChange(value: String) {
        _uiState.value = _uiState.value.copy(inquiryId = value)
    }

    fun onValidUntilChange(value: String) {
        _uiState.value = _uiState.value.copy(validUntil = value)
    }

    fun onTermsAndConditionsChange(value: String) {
        _uiState.value = _uiState.value.copy(termsAndConditions = value)
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onQuotationDiscountChange(value: String) {
        val parsed = value.trim().toDoubleOrNull()
        val error = if (value.isNotBlank() && (parsed == null || parsed < 0)) "Discount cannot be negative" else null
        _uiState.value = _uiState.value.copy(
            quotationDiscountText = value,
            discountError = error
        )
    }

    fun onPaymentTermsChange(terms: PaymentTerms) {
        _uiState.value = _uiState.value.copy(paymentTerms = terms)
    }

    fun onDeliveryRequirementChange(req: DeliveryRequirement?) {
        _uiState.value = _uiState.value.copy(deliveryRequirement = req)
    }

    // --- Items Management ---

    fun openAddItemDialog() {
        if (_uiState.value.isImmutableError) return
        _uiState.value = _uiState.value.copy(
            isItemDialogOpen = true,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun openEditItemDialog(index: Int) {
        if (_uiState.value.isImmutableError) return
        val currentItems = _uiState.value.items
        if (index in currentItems.indices) {
            _uiState.value = _uiState.value.copy(
                isItemDialogOpen = true,
                editingItemIndex = index,
                editingItem = currentItems[index]
            )
        }
    }

    fun closeItemDialog() {
        _uiState.value = _uiState.value.copy(
            isItemDialogOpen = false,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun saveItem(item: QuotationItem) {
        val currentList = _uiState.value.items.toMutableList()
        val index = _uiState.value.editingItemIndex

        if (index != null && index in currentList.indices) {
            currentList[index] = item
        } else {
            currentList.add(item)
        }

        _uiState.value = _uiState.value.copy(
            items = currentList,
            itemsError = null,
            isItemDialogOpen = false,
            editingItemIndex = null,
            editingItem = null
        )
    }

    fun removeItem(index: Int) {
        if (_uiState.value.isImmutableError) return
        val currentList = _uiState.value.items.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            _uiState.value = _uiState.value.copy(items = currentList)
        }
    }

    // --- Save Quotation ---

    fun saveQuotation(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.isSaving || state.isImmutableError) return

        var hasError = false
        val customerError = if (state.customerId.isBlank()) {
            hasError = true
            "Customer selection is required."
        } else null

        val itemsError = if (state.items.isEmpty()) {
            hasError = true
            "At least one commercial line item is required."
        } else null

        val discountParsed = state.quotationDiscountText.trim().toDoubleOrNull()
        val discountError = if (discountParsed == null || discountParsed < 0) {
            hasError = true
            "Quotation discount must be a valid positive amount or 0."
        } else null

        if (hasError) {
            _uiState.value = state.copy(
                customerIdError = customerError,
                itemsError = itemsError,
                discountError = discountError
            )
            return
        }

        scope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val timestamp = Instant.now().toString()
            val discountMoney = Money(discountParsed ?: 0.0)

            val result = if (formMode == QuotationFormMode.CREATE) {
                val newId = "quo-${UUID.randomUUID().toString().take(8)}"
                val newNumber = "QUO-${(1000..9999).random()}"
                val initialRevision = QuotationRevision(
                    revisionId = "rev-001",
                    quotationId = newId,
                    revisionNumber = 1,
                    items = state.items,
                    discount = discountMoney,
                    paymentTerms = state.paymentTerms,
                    deliveryRequirement = state.deliveryRequirement,
                    notes = state.notes.trim().ifBlank { null },
                    revisionReason = "Initial quotation creation",
                    createdAt = timestamp,
                    createdBy = "Commercial Desk"
                )

                val quotationToCreate = Quotation(
                    quotationId = newId,
                    quotationNumber = newNumber,
                    customerId = state.customerId.trim(),
                    inquiryId = state.inquiryId.trim().ifBlank { null },
                    status = QuotationStatusType.DRAFT,
                    currentRevisionNumber = 1,
                    revisions = listOf(initialRevision),
                    validUntil = state.validUntil.trim().ifBlank { null },
                    termsAndConditions = state.termsAndConditions.trim().ifBlank { null },
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
                quotationRepository.createQuotation(quotationToCreate)
            } else {
                val current = existingQuotation
                val currentRev = current?.currentRevision

                // Update the current draft revision in-place
                val updatedRevision = currentRev?.copy(
                    items = state.items,
                    discount = discountMoney,
                    paymentTerms = state.paymentTerms,
                    deliveryRequirement = state.deliveryRequirement,
                    notes = state.notes.trim().ifBlank { null }
                ) ?: QuotationRevision(
                    revisionId = "rev-001",
                    quotationId = checkNotNull(state.quotationId),
                    revisionNumber = 1,
                    items = state.items,
                    discount = discountMoney,
                    paymentTerms = state.paymentTerms,
                    deliveryRequirement = state.deliveryRequirement,
                    notes = state.notes.trim().ifBlank { null },
                    createdAt = timestamp,
                    createdBy = "Commercial Desk"
                )

                val updatedRevisions = current?.revisions?.map { rev ->
                    if (rev.revisionId == updatedRevision.revisionId) updatedRevision else rev
                } ?: listOf(updatedRevision)

                val quotationToUpdate = Quotation(
                    quotationId = checkNotNull(state.quotationId) { "Quotation ID cannot be null in edit mode." },
                    quotationNumber = current?.quotationNumber ?: state.quotationNumber.ifBlank { "QUO-0000" },
                    customerId = state.customerId.trim(),
                    inquiryId = state.inquiryId.trim().ifBlank { null },
                    status = QuotationStatusType.DRAFT,
                    currentRevisionNumber = current?.currentRevisionNumber ?: 1,
                    revisions = updatedRevisions,
                    approvedRevisionId = current?.approvedRevisionId,
                    approvedBy = current?.approvedBy,
                    approvedAt = current?.approvedAt,
                    validUntil = state.validUntil.trim().ifBlank { null },
                    termsAndConditions = state.termsAndConditions.trim().ifBlank { null },
                    createdAt = current?.createdAt ?: state.createdAt ?: timestamp,
                    updatedAt = timestamp
                )
                quotationRepository.updateQuotation(quotationToUpdate)
            }

            when (result) {
                is DomainResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        savedQuotationId = result.data.quotationId
                    )
                    onSuccess(result.data.quotationId)
                }
                is DomainResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = result.message
                    )
                }
                DomainResult.Loading -> {
                    // Handled by isSaving
                }
            }
        }
    }
}
