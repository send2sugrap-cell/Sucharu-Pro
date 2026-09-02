package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Line item on a Vendor Invoice (Module 12 Step 07).
 */
data class VendorInvoiceItem(
    val itemId: String,
    val invoiceId: String,
    val purchaseOrderItemId: String,
    val deliveryReceiptItemId: String? = null,
    val description: String,
    val quantity: BigDecimal,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val unitPrice: Money = Money.ZERO,
    val taxRate: BigDecimal = BigDecimal.ZERO,
    val taxAmount: Money = Money.ZERO,
    val discountAmount: Money = Money.ZERO,
    val lineTotal: Money = Money.ZERO,
    val sequence: Int = 1,
    val version: Long = 1L
)
