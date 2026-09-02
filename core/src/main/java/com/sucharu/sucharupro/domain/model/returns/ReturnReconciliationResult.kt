package com.sucharu.sucharupro.domain.model.returns

/**
 * Domain model representing the outcome of inventory reconciliation for a received return request.
 * (Module 11 Step 04 Chunk 04).
 *
 * Captures whether inventory stock-in and ledger entries were applied, or if zero accepted quantity
 * resulted in a record-only reconciliation.
 */
data class ReturnReconciliationResult(
    val returnId: String,
    val receivingEventId: String,
    val projectId: String,
    val acceptedQty: Int,
    val stockInRecordId: String?,
    val ledgerEntryId: String?,
    val inventoryMutationApplied: Boolean,
    val resultingStatus: ReturnStatus,
    val reconciledBy: String,
    val completedAt: Long = System.currentTimeMillis()
) {
    init {
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(receivingEventId.isNotBlank()) { "Receiving event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(acceptedQty >= 0) { "Accepted quantity cannot be negative." }
        require(reconciledBy.isNotBlank()) { "ReconciledBy actor cannot be blank." }
        if (inventoryMutationApplied) {
            require(!stockInRecordId.isNullOrBlank()) { "StockInRecordId must be provided when inventory mutation is applied." }
        }
    }
}
