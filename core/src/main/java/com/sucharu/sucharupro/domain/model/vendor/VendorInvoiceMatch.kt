package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Detailed line-level match comparison (Module 12 Step 07).
 */
data class VendorInvoiceMatchLine(
    val matchLineId: String,
    val matchId: String,
    val invoiceItemId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val description: String,
    val orderedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val invoicedQuantity: BigDecimal,
    val orderedUnitPrice: Money,
    val invoicedUnitPrice: Money,
    val quantityVariance: BigDecimal = BigDecimal.ZERO,
    val priceVariance: Money = Money.ZERO,
    val amountVariance: Money = Money.ZERO,
    val matchStatus: VendorInvoiceMatchStatus = VendorInvoiceMatchStatus.NOT_MATCHED,
    val exceptionReason: String? = null
)

/**
 * Result of 3-Way Matching between PO ↔ Delivery Receipt ↔ Vendor Invoice (Module 12 Step 07).
 */
data class VendorInvoiceMatch(
    val matchId: String,
    val projectId: String,
    val invoiceId: String,
    val purchaseOrderId: String,
    val matchStatus: VendorInvoiceMatchStatus = VendorInvoiceMatchStatus.NOT_MATCHED,
    val matchedAt: Long = System.currentTimeMillis(),
    val matchedBy: String = "system",
    val subtotalVariance: Money = Money.ZERO,
    val quantityVariance: BigDecimal = BigDecimal.ZERO,
    val priceVariance: Money = Money.ZERO,
    val taxVariance: Money = Money.ZERO,
    val totalVariance: Money = Money.ZERO,
    val currencyMismatch: Boolean = false,
    val vendorMismatch: Boolean = false,
    val unmatchedLineCount: Int = 0,
    val exceptionCount: Int = 0,
    val lines: List<VendorInvoiceMatchLine> = emptyList(),
    val version: Long = 1L
)
