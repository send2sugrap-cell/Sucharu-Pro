package com.sucharu.sucharupro.ui.features.orders.quotation.form

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.order.DeliveryRequirement
import com.sucharu.sucharupro.domain.model.order.PaymentTerms
import com.sucharu.sucharupro.domain.model.order.QuotationItem
import com.sucharu.sucharupro.domain.model.order.QuotationStatusType

/**
 * Operating mode of the Quotation Form.
 */
enum class QuotationFormMode {
    CREATE,
    EDIT_DRAFT
}

/**
 * UI State representation for Quotation Create and Edit forms.
 */
data class QuotationFormUiState(
    val mode: QuotationFormMode = QuotationFormMode.CREATE,
    val quotationId: String? = null,
    val quotationNumber: String = "",
    val customerId: String = "",
    val customerName: String? = null,
    val availableCustomers: List<Customer> = emptyList(),
    val inquiryId: String = "",
    val items: List<QuotationItem> = emptyList(),
    val quotationDiscountText: String = "0",
    val paymentTerms: PaymentTerms = PaymentTerms.DEFAULT,
    val deliveryRequirement: DeliveryRequirement? = null,
    val validUntil: String = "",
    val termsAndConditions: String = "Standard commercial terms apply. Prices valid for 30 days.",
    val notes: String = "",
    val status: QuotationStatusType = QuotationStatusType.DRAFT,
    val createdAt: String? = null,
    val isImmutableError: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val savedQuotationId: String? = null,
    val errorMessage: String? = null,
    // Field validation errors
    val customerIdError: String? = null,
    val itemsError: String? = null,
    val discountError: String? = null,
    // Item Dialog State
    val isItemDialogOpen: Boolean = false,
    val editingItemIndex: Int? = null,
    val editingItem: QuotationItem? = null
) {
    val isEditMode: Boolean get() = mode == QuotationFormMode.EDIT_DRAFT

    val subtotal: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.lineSubtotal }

    val discountMoney: Money
        get() {
            val parsed = quotationDiscountText.trim().toDoubleOrNull()
            return if (parsed != null && parsed >= 0) Money(parsed) else Money.ZERO
        }

    val totalAmount: Money
        get() {
            val sub = subtotal
            val disc = discountMoney
            return if (disc >= sub) Money.ZERO else sub - disc
        }

    val screenTitle: String get() = if (isEditMode) "Edit Draft Quotation" else "New Quotation"
    val saveButtonText: String get() = if (isEditMode) "Save Draft" else "Create Quotation"
}
