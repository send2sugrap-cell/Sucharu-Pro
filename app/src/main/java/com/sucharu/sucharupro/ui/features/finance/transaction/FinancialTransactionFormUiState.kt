package com.sucharu.sucharupro.ui.features.finance.transaction

import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType

data class FinancialTransactionFormUiState(
    val isSubmitting: Boolean = false,
    val transactionType: FinancialTransactionType = FinancialTransactionType.SALE,
    val entryType: FinancialEntryType = FinancialEntryType.DEBIT,
    val amountInput: String = "",
    val referenceType: FinancialReferenceType = FinancialReferenceType.ORDER,
    val referenceIdInput: String = "",
    val customerIdInput: String = "",
    val vendorIdInput: String = "",
    val descriptionInput: String = "",
    val notesInput: String = "",
    val errorMessage: String? = null,
    val successTransactionId: String? = null
)
