package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItem
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateGovernanceWorkItemAuditRecord

/**
 * Data Source Contract for Affiliate Command Center persistence.
 * Module 20 Step 05.
 */
interface AffiliateCommandCenterDataSource {
    suspend fun saveWorkItem(workItem: AffiliateGovernanceWorkItem): AffiliateGovernanceWorkItem
    suspend fun findWorkItemById(tenantId: String, workItemId: String): AffiliateGovernanceWorkItem?
    suspend fun listWorkItems(tenantId: String, affiliateId: String? = null): List<AffiliateGovernanceWorkItem>
    suspend fun saveAuditRecord(record: AffiliateGovernanceWorkItemAuditRecord): AffiliateGovernanceWorkItemAuditRecord
    suspend fun listAuditRecords(tenantId: String, affiliateId: String? = null): List<AffiliateGovernanceWorkItemAuditRecord>
    suspend fun getLastAuditChainHash(tenantId: String): String?
}
