package com.sucharu.sucharupro.domain.model.returns

/**
 * Root aggregate representing a Customer Return Request (Module 11 Step 01).
 *
 * Provides a canonical domain entity linking to original source transactions
 * (e.g. Challans) while isolating state and logic for RMA workflows.
 */
data class ReturnRequest(
    val returnId: String,
    val projectId: String,
    val returnNo: String,
    val customerId: String,
    val originalChallanId: String?,
    val status: ReturnStatus = ReturnStatus.REQUESTED,
    val reason: ReturnReason,
    val description: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val requestedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
) {
    init {
        require(returnId.isNotBlank()) { "Return ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(returnNo.isNotBlank()) { "Return Number cannot be blank." }
        require(customerId.isNotBlank()) { "Customer ID cannot be blank." }
        require(requestedBy.isNotBlank()) { "Requested By cannot be blank." }
        require(requestedAt > 0) { "Requested At timestamp must be strictly positive." }
        require(createdAt > 0) { "Created At timestamp must be strictly positive." }
        require(updatedAt >= createdAt) { "Updated At timestamp cannot precede Created At." }
        require(version > 0) { "Version must be strictly positive." }
    }
}
