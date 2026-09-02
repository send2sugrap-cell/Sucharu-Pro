package com.sucharu.sucharupro.domain.model.finance

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable settlement record generated upon posting a supplier payment (Module 09 Step 05).
 */
data class SupplierPaymentSettlement(
    val settlementId: String,
    val projectId: String,
    val paymentId: String,
    val payableId: String,
    val vendorId: String,
    val settledAmount: Money,
    val previousOutstanding: Money,
    val newOutstanding: Money,
    val settlementDate: Long = System.currentTimeMillis(),
    val financialTransactionId: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(settlementId.isNotBlank()) { "Settlement ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(paymentId.isNotBlank()) { "Payment ID cannot be blank." }
        require(payableId.isNotBlank()) { "Payable ID cannot be blank." }
        require(vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(settledAmount.isPositive()) { "Settled amount must be strictly positive (> 0)." }
        require(!previousOutstanding.isNegative()) { "Previous outstanding balance cannot be negative." }
        require(!newOutstanding.isNegative()) { "New outstanding balance cannot be negative." }
        require(settledAmount <= previousOutstanding) {
            "Settled amount (${settledAmount.formatted()}) cannot exceed previous outstanding balance (${previousOutstanding.formatted()})."
        }
        require(newOutstanding == previousOutstanding - settledAmount) {
            "New outstanding (${newOutstanding.formatted()}) must equal previous outstanding (${previousOutstanding.formatted()}) minus settled amount (${settledAmount.formatted()})."
        }
    }
}
