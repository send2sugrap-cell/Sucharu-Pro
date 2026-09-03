package com.sucharu.sucharupro.data.repository.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.AffiliateCommandCenterDataSource
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItem
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItemAuditRecord
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateCommandCenterRepository

/**
 * Repository Implementation delegating to AffiliateCommandCenterDataSource.
 * Module 20 Step 05.
 */
class AffiliateCommandCenterRepositoryImpl(
    private val dataSource: AffiliateCommandCenterDataSource
) : AffiliateCommandCenterRepository {

    override suspend fun saveWorkItem(workItem: AffiliateGovernanceWorkItem): AffiliateGovernanceWorkItem {
        return dataSource.saveWorkItem(workItem)
    }

    override suspend fun findWorkItemById(tenantId: String, workItemId: String): AffiliateGovernanceWorkItem? {
        return dataSource.findWorkItemById(tenantId, workItemId)
    }

    override suspend fun listWorkItems(tenantId: String, affiliateId: String?): List<AffiliateGovernanceWorkItem> {
        return dataSource.listWorkItems(tenantId, affiliateId)
    }

    override suspend fun saveAuditRecord(auditRecord: AffiliateGovernanceWorkItemAuditRecord): AffiliateGovernanceWorkItemAuditRecord {
        return dataSource.saveAuditRecord(auditRecord)
    }

    override suspend fun listAuditRecords(tenantId: String, affiliateId: String?): List<AffiliateGovernanceWorkItemAuditRecord> {
        return dataSource.listAuditRecords(tenantId, affiliateId)
    }

    override suspend fun getLastAuditChainHash(tenantId: String): String? {
        return dataSource.getLastAuditChainHash(tenantId)
    }
}
