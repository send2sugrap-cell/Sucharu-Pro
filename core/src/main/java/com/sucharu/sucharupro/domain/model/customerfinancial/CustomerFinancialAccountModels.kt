package com.sucharu.sucharupro.domain.model.customerfinancial

/**
 * Status lifecycle of a Customer Financial Account (Module 14 Step 01).
 */
enum class CustomerFinancialAccountStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED;

    val isActive: Boolean get() = this == ACTIVE
    val canTransact: Boolean get() = this == ACTIVE

    fun canTransitionTo(target: CustomerFinancialAccountStatus): Boolean {
        if (this == target) return true
        return when (this) {
            ACTIVE -> target in setOf(SUSPENDED, CLOSED)
            SUSPENDED -> target in setOf(ACTIVE, CLOSED)
            CLOSED -> false // Terminal state
        }
    }
}

/**
 * Customer Financial Account aggregate root (Module 14 Step 01).
 *
 * Serves as the authoritative financial account entity under which all future
 * invoices, advances, payments, allocations, refunds, adjustments, and ledger entries are associated.
 */
data class CustomerFinancialAccount(
    val financialAccountId: String,
    val tenantId: String,
    val projectId: String,
    val customerId: String,
    val accountNumber: String,
    val currency: String = "BDT",
    val status: CustomerFinancialAccountStatus = CustomerFinancialAccountStatus.ACTIVE,
    val suspensionReason: String? = null,
    val closedReason: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Immutable audit event for Customer Financial Account lifecycle and status modifications.
 */
data class CustomerFinancialAccountAuditEvent(
    val auditId: String,
    val financialAccountId: String,
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: CustomerFinancialAccountStatus? = null,
    val newStatus: CustomerFinancialAccountStatus? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)
