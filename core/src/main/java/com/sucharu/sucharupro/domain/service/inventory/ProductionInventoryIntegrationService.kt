package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import com.sucharu.sucharupro.domain.model.inventory.ProductionInventoryEligibility

/**
 * Authoritative integration service bridging Production Jobs & Final QC Release to Finished Goods Inventory (Phase 06 Step 01).
 */
interface ProductionInventoryIntegrationService {

    /**
     * Evaluates if a given production execution job is eligible for Finished Goods Inventory receiving.
     */
    suspend fun evaluateInventoryEligibility(
        tenantId: String,
        executionJobId: String
    ): DomainResult<ProductionInventoryEligibility>

    /**
     * Idempotently receives eligible finished goods output into Finished Product Inventory.
     * Received quantity is derived strictly from authoritative Final QC inspection / certified release data.
     */
    suspend fun receiveFinishedGoodsFromProduction(
        tenantId: String,
        executionJobId: String,
        warehouseId: String,
        binId: String? = null,
        actor: String,
        idempotencyKey: String? = null
    ): DomainResult<FinishedProductInventoryReceipt>

    /**
     * Retrieves an existing finished goods receipt for a specific production job.
     */
    suspend fun getFinishedGoodsReceiptForJob(
        tenantId: String,
        executionJobId: String
    ): DomainResult<FinishedProductInventoryReceipt?>
}
