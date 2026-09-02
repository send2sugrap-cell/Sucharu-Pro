package com.sucharu.sucharupro.domain.model.delivery.reconciliation

/**
 * Summary metrics for Delivery Reconciliation in project scope (Module 08 Step 09).
 */
data class DeliveryReconciliationSummary(
    val projectId: String,
    val totalReconciliations: Int = 0,
    val openCount: Int = 0,
    val inProgressCount: Int = 0,
    val partiallyReconciledCount: Int = 0,
    val requiresReviewCount: Int = 0,
    val reconciledCount: Int = 0,
    val disputedCount: Int = 0,
    val resolvedCount: Int = 0,
    val closedCount: Int = 0,
    val totalDiscrepancyCount: Int = 0
)
