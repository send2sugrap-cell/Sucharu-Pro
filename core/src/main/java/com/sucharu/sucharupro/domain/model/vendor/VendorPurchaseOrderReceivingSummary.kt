package com.sucharu.sucharupro.domain.model.vendor

import java.math.BigDecimal

/**
 * Authoritative summary of receiving progress against a Vendor Purchase Order (Module 12 Step 06).
 */
data class VendorPurchaseOrderReceivingSummary(
    val purchaseOrderId: String,
    val projectId: String,
    val totalOrderedQuantity: BigDecimal,
    val totalReceivedQuantity: BigDecimal,
    val totalAcceptedQuantity: BigDecimal,
    val totalRejectedQuantity: BigDecimal,
    val totalDamagedQuantity: BigDecimal,
    val totalShortQuantity: BigDecimal,
    val remainingReceivableQuantity: BigDecimal,
    val receiptCount: Int,
    val isFullyReceived: Boolean,
    val lastReceiptDate: Long? = null
)
