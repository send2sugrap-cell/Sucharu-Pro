package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilityReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.VendorProfitabilitySnapshot
import com.sucharu.sucharupro.domain.model.profitability.VendorSourceCollectionResult

/**
 * Non-mutating Mathematical Reconciliation Engine for Vendor Profitability.
 * Module 16 Step 05.
 */
interface VendorProfitabilityReconciliationService {

    suspend fun reconcile(
        snapshot: VendorProfitabilitySnapshot,
        sourceData: VendorSourceCollectionResult
    ): VendorProfitabilityReconciliationEvent
}
