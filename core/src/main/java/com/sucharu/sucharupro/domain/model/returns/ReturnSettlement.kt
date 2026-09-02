package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Immutable root aggregate representing the commercial/financial settlement and final disposition
 * of a processed Customer Return Request (Module 11 Step 05).
 */
data class ReturnSettlement(
    val settlementId: String,
    val returnId: String,
    val projectId: String,
    val customerId: String,
    val resolutionType: ReturnResolutionType,
    val amount: Money,
    val status: ReturnSettlementStatus = ReturnSettlementStatus.COMPLETED,
    val creditNoteId: String? = null,
    val replacementOrderId: String? = null,
    val reworkId: String? = null,
    val notes: String? = null,
    val settledBy: String,
    val settledAt: Long = System.currentTimeMillis(),
    val version: Long = 1L,
    val idempotencyKey: String
) {
    init {
        require(settlementId.isNotBlank()) { "Settlement ID cannot be blank." }
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(!amount.isNegative()) { "Settlement amount cannot be negative." }
        require(settledBy.isNotBlank()) { "Settled By cannot be blank." }
        require(settledAt > 0) { "Settled At timestamp must be positive." }
        require(version > 0) { "Version must be strictly positive (was $version)." }
        require(idempotencyKey.isNotBlank()) { "Idempotency key cannot be blank." }
    }
}
