package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.inventory.FinishedProductInventoryReceipt
import java.util.concurrent.ConcurrentHashMap

class FakeFinishedProductInventoryDataSource : FinishedProductInventoryDataSource {
    private val receipts = ConcurrentHashMap<String, MutableMap<String, FinishedProductInventoryReceipt>>()

    override suspend fun saveReceipt(tenantId: String, receipt: FinishedProductInventoryReceipt) {
        val tenantMap = receipts.computeIfAbsent(tenantId) { ConcurrentHashMap() }
        tenantMap.putIfAbsent(receipt.executionJobId, receipt)
    }

    override suspend fun getReceiptByJob(
        tenantId: String,
        executionJobId: String
    ): FinishedProductInventoryReceipt? {
        return receipts[tenantId]?.get(executionJobId)
    }

    override suspend fun listReceipts(tenantId: String): List<FinishedProductInventoryReceipt> {
        return receipts[tenantId]?.values?.sortedByDescending { it.receivedAt } ?: emptyList()
    }
}
