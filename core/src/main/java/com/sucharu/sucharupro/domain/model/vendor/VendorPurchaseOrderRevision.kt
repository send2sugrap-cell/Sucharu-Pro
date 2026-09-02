package com.sucharu.sucharupro.domain.model.vendor

import com.sucharu.sucharupro.domain.model.common.Money

/**
 * Historical amendment and revision record for a Purchase Order (Module 12 Step 05).
 */
data class VendorPurchaseOrderRevision(
    val revisionId: String,
    val projectId: String,
    val purchaseOrderId: String,
    val revisionNumber: Int,
    val previousTotalAmount: Money,
    val newTotalAmount: Money,
    val changeSummary: String,
    val revisedBy: String,
    val revisedAt: Long = System.currentTimeMillis()
)
