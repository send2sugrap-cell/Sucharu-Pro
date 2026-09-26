package com.sucharu.sucharupro.domain.service.quotationlock

import com.sucharu.sucharupro.domain.model.quotationlock.*
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BI-03 Domain Service for Quotation -> Order Commercial Lock & Amendment Governance.
 */
class CommercialLockService {

    private val lockStore = ConcurrentHashMap<String, CommercialLockBaseline>()
    private val amendmentStore = ConcurrentHashMap<String, CommercialAmendmentRequest>()

    /**
     * Locks commercial terms upon customer acceptance of quotation version [versionNumber].
     * (CRITICAL INVARIANT: Creates immutable CommercialLockBaseline. Order price snapshot remains 100% immutable!)
     */
    fun acceptAndLockQuotation(
        quoteId: String,
        quoteNumber: String = "QUOTE-2026-001",
        versionNumber: Int = 1,
        customerId: String = "CUST-1001",
        orderId: String = "ORD-1001",
        orderPriceSnapshotId: String = "SNAP-1001",
        agreedTotal: BigDecimal = BigDecimal("410.00"),
        acceptanceChannel: String = "CUSTOMER_PORTAL",
        actorId: String = "CUST-1001"
    ): CommercialLockBaseline {
        val lockId = "LOCK-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T19:30:00Z"

        val acceptance = CustomerAcceptanceRecord(
            acceptedBy = customerId,
            acceptedAt = timestamp,
            acceptanceChannel = acceptanceChannel,
            customerNotes = "Approved via Customer Portal",
            acceptedVersionNumber = versionNumber
        )

        val lockBaseline = CommercialLockBaseline(
            lockId = lockId,
            quoteId = quoteId,
            quoteNumber = quoteNumber,
            acceptedVersionNumber = versionNumber,
            orderId = orderId,
            orderPriceSnapshotId = orderPriceSnapshotId,
            lockState = CommercialLockState.COMMERCIAL_LOCKED,
            customerAcceptance = acceptance,
            agreedGrandTotal = agreedTotal,
            agreedCurrency = "BDT",
            isImmutable = true, // ALWAYS TRUE! (Critical Invariant)
            lockedAt = timestamp,
            lockedBy = actorId
        )

        lockStore[orderId] = lockBaseline
        return lockBaseline
    }

    /**
     * Submits an authorized Post-Lock Commercial Amendment Request.
     * (CRITICAL INVARIANT: Does NOT mutate existing locked OrderPriceSnapshot!)
     */
    fun requestCommercialAmendment(
        lockId: String,
        orderId: String,
        requestedBy: String,
        reason: String,
        changesJson: String
    ): CommercialAmendmentRequest {
        val amendmentId = "AMD-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T19:35:00Z"

        val amendment = CommercialAmendmentRequest(
            amendmentId = amendmentId,
            lockId = lockId,
            orderId = orderId,
            revisionNumber = 2,
            requestedBy = requestedBy,
            reason = reason,
            requestedChangesJson = changesJson,
            status = "PENDING",
            requestedAt = timestamp
        )

        amendmentStore[amendmentId] = amendment
        return amendment
    }

    fun getCommercialBaselineByOrder(orderId: String): CommercialLockBaseline? {
        return lockStore[orderId]
    }
}
