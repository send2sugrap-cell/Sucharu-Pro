package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money
import java.math.BigDecimal

/**
 * Line item on a Vendor Delivery Receipt (Module 12 Step 06).
 */
data class VendorDeliveryReceiptItem(
    val receiptItemId: String,
    val deliveryReceiptId: String,
    val purchaseOrderId: String,
    val purchaseOrderItemId: String,
    val itemDescription: String,
    val itemCode: String? = null,
    val orderedQuantity: BigDecimal,
    val previouslyReceivedQuantity: BigDecimal = BigDecimal.ZERO,
    val receivedQuantity: BigDecimal,
    val acceptedQuantity: BigDecimal = BigDecimal.ZERO,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val damagedQuantity: BigDecimal = BigDecimal.ZERO,
    val shortQuantity: BigDecimal = BigDecimal.ZERO,
    val excessQuantity: BigDecimal = BigDecimal.ZERO,
    val unitOfMeasure: UnitOfMeasure = UnitOfMeasure.PIECE,
    val unitRate: Money = Money.ZERO,
    val taxAmount: Money = Money.ZERO,
    val lineTotal: Money = Money.ZERO,
    val remarks: String? = null,
    val version: Long = 1L
)
