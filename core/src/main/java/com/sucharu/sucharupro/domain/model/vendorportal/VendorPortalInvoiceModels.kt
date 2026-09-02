package com.sucharu.sucharupro.domain.model.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceMatchStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorInvoiceStatus
import java.math.BigDecimal

/**
 * Status of a vendor-submitted invoice draft / request (Module 13 Step 06).
 */
enum class VendorPortalInvoiceSubmissionStatus {
    DRAFT,
    SUBMITTED,
    CONVERTED,
    REJECTED,
    CANCELLED
}

/**
 * Categorization of vendor response to match exceptions or invoice queries.
 */
enum class VendorPortalInvoiceResponseType {
    CLARIFY_EXCEPTION,
    ACCEPT_VARIANCE,
    DISPUTE_VARIANCE,
    PROPOSE_CORRECTION,
    SUBMIT_ADDITIONAL_DOCS
}

/**
 * Financial evidence and document types.
 */
enum class VendorPortalFinancialEvidenceType {
    INVOICE_DOCUMENT,
    TAX_DOCUMENT,
    DELIVERY_PROOF,
    CLARIFICATION_ATTACHMENT,
    PAYMENT_REMITTANCE,
    DISPUTE_EVIDENCE
}

/**
 * Payment and settlement projection status.
 */
enum class VendorPortalPaymentStatus {
    PENDING,
    APPROVED,
    PARTIALLY_PAID,
    PAID,
    ON_HOLD,
    DISPUTED,
    REJECTED,
    CANCELLED
}

/**
 * Item in a vendor-initiated invoice submission.
 */
data class VendorPortalInvoiceSubmissionItem(
    val itemId: String,
    val submissionId: String,
    val tenantId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val itemName: String,
    val itemCode: String? = null,
    val invoicedQuantity: BigDecimal,
    val unitOfMeasure: String = "PIECE",
    val unitPrice: Money,
    val taxAmount: Money = Money.ZERO,
    val lineTotal: Money,
    val remarks: String? = null
)

/**
 * Input for creating/updating a submission item.
 */
data class VendorPortalInvoiceSubmissionItemInput(
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val invoicedQuantity: BigDecimal,
    val unitPrice: Money? = null,
    val taxAmount: Money? = null,
    val remarks: String? = null
)

/**
 * Vendor-initiated invoice submission aggregate.
 */
data class VendorPortalInvoiceSubmission(
    val submissionId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val subtotalAmount: Money = Money.ZERO,
    val taxAmount: Money = Money.ZERO,
    val discountAmount: Money = Money.ZERO,
    val shippingAmount: Money = Money.ZERO,
    val otherCharges: Money = Money.ZERO,
    val totalAmount: Money = Money.ZERO,
    val notes: String? = null,
    val status: VendorPortalInvoiceSubmissionStatus = VendorPortalInvoiceSubmissionStatus.DRAFT,
    val canonicalInvoiceId: String? = null,
    val rejectionReason: String? = null,
    val items: List<VendorPortalInvoiceSubmissionItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String,
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = createdBy,
    val submittedAt: Long? = null,
    val submittedBy: String? = null,
    val version: Long = 1L
)

/**
 * Vendor-facing projection of a canonical Vendor Invoice.
 */
data class VendorPortalInvoiceSummary(
    val invoiceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val purchaseOrderId: String,
    val orderNumber: String,
    val invoiceNumber: String,
    val vendorInvoiceNumber: String,
    val invoiceDate: Long,
    val receivedDate: Long,
    val currency: String,
    val subtotal: Money,
    val taxAmount: Money,
    val discountAmount: Money,
    val shippingAmount: Money,
    val otherCharges: Money,
    val totalAmount: Money,
    val approvedAmount: Money,
    val paidAmount: Money,
    val outstandingAmount: Money,
    val status: VendorInvoiceStatus,
    val matchStatus: VendorInvoiceMatchStatus,
    val paymentStatus: VendorPortalPaymentStatus,
    val exceptionCount: Int = 0,
    val notes: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Vendor-friendly line in 3-way match breakdown.
 */
data class VendorPortalInvoiceMatchLineSummary(
    val matchLineId: String,
    val invoiceItemId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String?,
    val description: String,
    val orderedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal,
    val invoicedQuantity: BigDecimal,
    val orderedUnitPrice: Money,
    val invoicedUnitPrice: Money,
    val quantityVariance: BigDecimal,
    val priceVariance: Money,
    val amountVariance: Money,
    val matchStatus: VendorInvoiceMatchStatus,
    val exceptionReason: String? = null
)

/**
 * Vendor-friendly 3-Way Match Summary.
 */
data class VendorPortalInvoiceMatchSummary(
    val matchId: String,
    val invoiceId: String,
    val purchaseOrderId: String,
    val matchStatus: VendorInvoiceMatchStatus,
    val matchedAt: Long,
    val subtotalVariance: Money,
    val quantityVariance: BigDecimal,
    val priceVariance: Money,
    val taxVariance: Money,
    val totalVariance: Money,
    val currencyMismatch: Boolean,
    val vendorMismatch: Boolean,
    val exceptionCount: Int,
    val lines: List<VendorPortalInvoiceMatchLineSummary> = emptyList()
)

/**
 * Vendor response to match exception or clarification request.
 */
data class VendorPortalInvoiceResponse(
    val responseId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val invoiceId: String,
    val exceptionId: String? = null,
    val responseType: VendorPortalInvoiceResponseType,
    val comment: String,
    val proposedCorrection: String? = null,
    val evidenceReferences: List<String> = emptyList(),
    val respondedBy: String,
    val respondedAt: Long = System.currentTimeMillis(),
    val version: Long = 1L
)

/**
 * Secure financial evidence record.
 */
data class VendorPortalFinancialEvidence(
    val evidenceId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val evidenceType: VendorPortalFinancialEvidenceType,
    val filename: String,
    val fileReference: String,
    val fileHash: String? = null,
    val mimeType: String = "application/pdf",
    val sizeBytes: Long,
    val uploadedBy: String,
    val uploadedAt: Long = System.currentTimeMillis()
)

/**
 * Vendor-facing payment/settlement projection.
 */
data class VendorPortalPaymentSummary(
    val settlementId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val settlementNumber: String,
    val settlementDate: Long,
    val currency: String,
    val totalAmount: Money,
    val paymentStatus: VendorPortalPaymentStatus,
    val paymentMethod: String, // Masked/formatted
    val referenceNumber: String?, // Masked e.g. ****4821
    val relatedInvoiceIds: List<String> = emptyList(),
    val notes: String? = null,
    val settledAt: Long? = null
)

/**
 * High-level vendor financial KPI aggregate.
 */
data class VendorPortalFinancialKpiSummary(
    val vendorId: String,
    val currency: String,
    val totalInvoiced: Money,
    val totalApproved: Money,
    val totalPaid: Money,
    val totalOutstanding: Money,
    val totalDisputed: Money,
    val totalOnHold: Money,
    val invoiceCount: Int,
    val outstandingInvoiceCount: Int,
    val paidInvoiceCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Financial activity event projection for timeline.
 */
data class VendorPortalFinancialActivity(
    val activityId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val entityType: String,
    val entityId: String,
    val eventType: String,
    val title: String,
    val description: String,
    val amount: Money? = null,
    val actorId: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Immutable audit event for portal financial actions.
 */
data class VendorPortalInvoiceAuditEvent(
    val auditId: String,
    val tenantId: String,
    val projectId: String,
    val vendorId: String,
    val targetType: String,
    val targetId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val correlationId: String? = null,
    val payload: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
