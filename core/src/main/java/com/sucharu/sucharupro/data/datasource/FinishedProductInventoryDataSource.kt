package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt

/**
 * DataSource interface for Finished Product Inventory Receipts (Phase 06 Step 01).
 */
interface FinishedProductInventoryDataSource {
    suspend fun saveReceipt(tenantId: String, receipt: FinishedProductInventoryReceipt)
    suspend fun getReceiptByJob(tenantId: String, executionJobId: String): FinishedProductInventoryReceipt?
    suspend fun listReceipts(tenantId: String): List<FinishedProductInventoryReceipt>
}
