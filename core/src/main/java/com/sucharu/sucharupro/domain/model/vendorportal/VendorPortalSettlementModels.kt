package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.SettlementMethod
import com.sucharu.sucharupro.domain.model.vendor.VendorSettlementStatus
import java.math.BigDecimal

/**
 * Status of a vendor's acknowledgement of a settlement (Module 13 Step 09).
 */
enum class VendorPortalSettlementViewStatus {
    VIEW_ONLY,
    ACKNOWLEDGED,
    ACKNOWLEDGED_WITH_DISCREPANCY,
    DECLINED
}

/**
 * Status of vendor reconciliation query cases.
 */
enum class VendorPortalReconciliationCaseStatus {
    OPEN,
    UNDER_REVIEW,
    VENDOR_RESPONSE_REQUIRED,
    INTERNAL_RESPONSE_REQUIRED,
    RESOLVED,
    CLOSED,
    CANCELLED
}

/**
 * Lifecycle status of financial dispute records in the vendor portal.
 */
enum class VendorPortalFinancialDisputeStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    RESPONSE_REQUIRED,
    RESOLUTION_PROPOSED,
    RESOLVED,
    CLOSED,
    REJECTED
}

/**
 * Types of financial evidence supporting settlement and reconciliation.
 */
enum class VendorPortalSettlementEvidenceType {
    SETTLEMENT_STATEMENT,
    BANK_ADVICE,
    RECONCILIATION_PROOF,
    DISPUTE_JUSTIFICATION,
    TAX_WITHHOLDING_CERT,
    CREDIT_MEMO,
    OTHER
}

/**
 * Types of financial activity events.
 */
enum class VendorPortalFinancialActivityEventType {
    SETTLEMENT_PROJECTION_VIEWED,
    SETTLEMENT_ACKNOWLEDGED,
    RECONCILIATION_QUERY_CREATED,
    RECONCILIATION_RESPONSE_SUBMITTED,
    RECONCILIATION_RESOLVED,
    DISPUTE_OPENED,
    DISPUTE_RESPONSE_POSTED,
    DISPUTE_RESOLVED,
    EVIDENCE_ATTACHED,
    MESSAGE_SENT
}

/**
 * Vendor-facing projection of a canonical Vendor Settlement.
 * Masked payment information only; no internal bank secrets exposed.
 */
data class VendorPortalSettlementSummary(
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long,
    val currency: String = "BDT",
    val grossAmount: Money = Money.ZERO,
    val deductions: Money = Money.ZERO,
    val credits: Money = Money.ZERO,
    val netPayable: Money = Money.ZERO,
    val status: VendorSettlementStatus = VendorSettlementStatus.DRAFT,
    val settlementMethod: SettlementMethod = SettlementMethod.BANK_TRANSFER,
    val maskedPaymentReference: String? = null,
    val notes: String? = null,
    val approvedAt: Long? = null,
    val settledAt: Long? = null,
    val allocationCount: Int = 0,
    val acknowledgementStatus: VendorPortalSettlementViewStatus = VendorPortalSettlementViewStatus.VIEW_ONLY
)

/**
 * Vendor-facing allocation line projection breaking down a settlement across invoices and POs.
 */
data class VendorPortalSettlementAllocationProjection(
    val allocationId: String,
    val settlementId: String,
    val payableId: String,
    val invoiceId: String? = null,
    val invoiceNumber: String? = null,
    val purchaseOrderId: String? = null,
    val orderNumber: String? = null,
    val allocatedAmount: Money = Money.ZERO,
    val currency: String = "BDT",
    val allocatedAt: Long = System.currentTimeMillis()
)

/**
 * Idempotent vendor settlement acknowledgement record.
 */
data class VendorPortalSettlementAcknowledgement(
    val acknowledgementId: String,
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val acknowledgedBy: String,
    val acknowledgedAt: Long = System.currentTimeMillis(),
    val status: VendorPortalSettlementViewStatus = VendorPortalSettlementViewStatus.ACKNOWLEDGED,
    val idempotencyKey: String,
    val discrepancyFlag: Boolean = false,
    val discrepancyNotes: String? = null,
    val evidenceReferences: List<String> = emptyList()
)

/**
 * Vendor-initiated or internal reconciliation inquiry case.
 */
data class VendorPortalReconciliationCase(
    val caseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val caseNumber: String,
    val subject: String,
    val status: VendorPortalReconciliationCaseStatus = VendorPortalReconciliationCaseStatus.OPEN,
    val claimedAmount: Money = Money.ZERO,
    val systemAmount: Money = Money.ZERO,
    val varianceAmount: Money = Money.ZERO,
    val currency: String = "BDT",
    val notes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val events: List<VendorPortalReconciliationEvent> = emptyList()
)

/**
 * Immutable chronological event on a reconciliation case.
 */
data class VendorPortalReconciliationEvent(
    val eventId: String,
    val caseId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val remarks: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Vendor financial dispute aggregate record with Separation of Duties enforcement.
 */
data class VendorPortalFinancialDispute(
    val disputeId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementId: String? = null,
    val invoiceId: String? = null,
    val disputeNumber: String,
    val category: String,
    val priority: String = "NORMAL",
    val status: VendorPortalFinancialDisputeStatus = VendorPortalFinancialDisputeStatus.SUBMITTED,
    val disputedAmount: Money = Money.ZERO,
    val proposedResolutionAmount: Money? = null,
    val currency: String = "BDT",
    val reason: String,
    val resolutionNotes: String? = null,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedBy: String? = null,
    val resolvedAt: Long? = null,
    val events: List<VendorPortalFinancialDisputeEvent> = emptyList()
)

/**
 * Immutable chronological event on a financial dispute.
 */
data class VendorPortalFinancialDisputeEvent(
    val eventId: String,
    val disputeId: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val remarks: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Supporting financial evidence record for settlement, reconciliation, or dispute.
 */
data class VendorPortalFinancialSettlementEvidence(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val evidenceType: VendorPortalSettlementEvidenceType = VendorPortalSettlementEvidenceType.SETTLEMENT_STATEMENT,
    val fileName: String,
    val fileUrl: String,
    val checksum: String? = null,
    val fileSizeBytes: Long = 0L,
    val mimeType: String? = "application/pdf",
    val description: String? = null,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Real-time or asynchronous financial collaboration thread.
 */
data class VendorPortalFinancialThread(
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val contextType: String, // SETTLEMENT, RECONCILIATION, DISPUTE
    val contextId: String,
    val subject: String,
    val status: String = "ACTIVE",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
)

/**
 * Message within a financial collaboration thread.
 */
data class VendorPortalFinancialMessage(
    val messageId: String,
    val threadId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val senderId: String,
    val senderRole: String,
    val content: String,
    val evidenceReferences: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Activity timeline projection event.
 */
data class VendorPortalFinancialActivityEvent(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val eventType: VendorPortalFinancialActivityEventType,
    val entityType: String,
    val entityId: String,
    val actorId: String,
    val actorRole: String,
    val description: String,
    val occurredAt: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Consolidated analytics snapshot for the vendor's financial workspace.
 */
data class VendorPortalSettlementAnalyticsSummary(
    val vendorId: String,
    val currency: String = "BDT",
    val totalSettledAmount: Money = Money.ZERO,
    val totalOutstandingAmount: Money = Money.ZERO,
    val totalDisputedAmount: Money = Money.ZERO,
    val totalReconciledAmount: Money = Money.ZERO,
    val activeDisputeCount: Int = 0,
    val pendingReconciliationCount: Int = 0,
    val averageSettlementCycleDays: Double = 0.0,
    val disputeResolutionRate: Double = 100.0
)

/**
 * Consolidated workspace payload for the Vendor Settlement & Financial Collaboration Hub.
 */
data class VendorPortalFinancialWorkspace(
    val settlementOverview: List<VendorPortalSettlementSummary> = emptyList(),
    val outstandingBalance: Money = Money.ZERO,
    val pendingReconciliations: List<VendorPortalReconciliationCase> = emptyList(),
    val openDisputes: List<VendorPortalFinancialDispute> = emptyList(),
    val recentActivity: List<VendorPortalFinancialActivityEvent> = emptyList(),
    val analytics: VendorPortalSettlementAnalyticsSummary
)
