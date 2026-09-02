package com.sucharu.sucharupro.data.datasource.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditControlAuditEvent
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditProfileEntity

/**
 * Data Source contract for Customer Credit Profiles & Audit Events (Module 14 Step 07).
 */
interface CustomerCreditControlDataSource {
    suspend fun saveProfile(profile: CustomerCreditProfileEntity): DomainResult<CustomerCreditProfileEntity>
    suspend fun getProfileByCustomerId(tenantId: String, projectId: String, customerId: String): DomainResult<CustomerCreditProfileEntity?>
    suspend fun listProfiles(tenantId: String, projectId: String, financialHold: Boolean? = null, limit: Int = 50, offset: Int = 0): DomainResult<List<CustomerCreditProfileEntity>>
    suspend fun recordAuditEvent(event: CustomerCreditControlAuditEvent): DomainResult<CustomerCreditControlAuditEvent>
    suspend fun getAuditEvents(tenantId: String, projectId: String, customerId: String): DomainResult<List<CustomerCreditControlAuditEvent>>
}
