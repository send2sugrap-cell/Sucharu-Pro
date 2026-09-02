package com.sucharu.sucharupro.domain.model.customerpayment

import java.math.BigDecimal

/**
 * Supported payment methods for Customer Payments (Module 14 Step 03).
 */
enum class CustomerPaymentMethod {
    CASH,
    BKASH,
    NAGAD,
    BANK,
    OTHER;

    val isDigitalWallet: Boolean get() = this in setOf(BKASH, NAGAD)
    val requiresReference: Boolean get() = this in setOf(BKASH, NAGAD, BANK)
}

/**
 * Lifecycle status for Customer Payments (Module 14 Step 03).
 */
enum class CustomerPaymentStatus {
    RECORDED,
    CONFIRMED,
    CANCELLED;

    val isRecorded: Boolean get() = this == RECORDED
    val isConfirmed: Boolean get() = this == CONFIRMED
    val isCancelled: Boolean get() = this == CANCELLED
    val isTerminal: Boolean get() = this == CANCELLED

    fun canTransitionTo(target: CustomerPaymentStatus): Boolean {
        if (this == target) return true
        return when (this) {
            RECORDED -> target in setOf(CONFIRMED, CANCELLED)
            CONFIRMED -> target == CANCELLED
            CANCELLED -> false // Terminal state
        }
    }
}

/**
 * Customer Payment aggregate root (Module 14 Step 03).
 *
 * Establishes the authoritative financial record of a payment made by a Customer,
 * linked to the CustomerFinancialAccount and optionally directly to a CustomerInvoice.
 */
data class CustomerPayment(
    val paymentId: String,
    val tenantId: String,
    val projectId: String,
    val paymentNumber: String,
    val customerId: String,
    val customerFinancialAccountId: String,
    val invoiceId: String? = null,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val paymentMethod: CustomerPaymentMethod = CustomerPaymentMethod.CASH,
    val paymentDate: Long = System.currentTimeMillis(),
    val referenceNumber: String? = null,
    val externalReference: String? = null,
    val notes: String? = null,
    val status: CustomerPaymentStatus = CustomerPaymentStatus.RECORDED,
    val idempotencyKey: String? = null,
    val cancellationReason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Immutable financial audit event for Customer Payment operations.
 */
data class CustomerPaymentAuditEvent(
    val auditId: String,
    val paymentId: String,
    val customerId: String,
    val tenantId: String,
    val projectId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val previousStatus: CustomerPaymentStatus? = null,
    val newStatus: CustomerPaymentStatus? = null,
    val reason: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null
)
