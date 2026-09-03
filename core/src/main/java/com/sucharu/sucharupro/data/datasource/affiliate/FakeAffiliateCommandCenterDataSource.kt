package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItem
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItemAuditRecord
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-Safe In-Memory Fake Data Source for Affiliate Command Center.
 * Module 20 Step 05.
 */
class FakeAffiliateCommandCenterDataSource : AffiliateCommandCenterDataSource {

    private val workItems = ConcurrentHashMap<String, MutableMap<String, AffiliateGovernanceWorkItem>>()
    private val auditRecords = ConcurrentHashMap<String, MutableList<AffiliateGovernanceWorkItemAuditRecord>>()

    override suspend fun saveWorkItem(workItem: AffiliateGovernanceWorkItem): AffiliateGovernanceWorkItem {
        val tenantMap = workItems.computeIfAbsent(workItem.tenantId) { ConcurrentHashMap() }
        tenantMap[workItem.workItemId] = workItem
        return workItem
    }

    override suspend fun findWorkItemById(tenantId: String, workItemId: String): AffiliateGovernanceWorkItem? {
        return workItems[tenantId]?.get(workItemId)
    }

    override suspend fun listWorkItems(tenantId: String, affiliateId: String?): List<AffiliateGovernanceWorkItem> {
        val list = workItems[tenantId]?.values?.toList() ?: emptyList()
        return if (affiliateId != null) list.filter { it.affiliateId == affiliateId } else list
    }

    override suspend fun saveAuditRecord(record: AffiliateGovernanceWorkItemAuditRecord): AffiliateGovernanceWorkItemAuditRecord {
        val tenantList = auditRecords.computeIfAbsent(record.tenantId) { mutableListOf() }
        synchronized(tenantList) {
            tenantList.add(record)
        }
        return record
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String?): List<AffiliateGovernanceWorkItemAuditRecord> {
        val list = auditRecords[tenantId]?.toList() ?: emptyList()
        return if (affiliateId != null) list.filter { it.affiliateId == affiliateId } else list
    }

    override suspend fun getLastAuditChainHash(tenantId: String): String? {
        val list = auditRecords[tenantId] ?: return null
        synchronized(list) {
            return list.lastOrNull()?.chainHash
        }
    }
}
