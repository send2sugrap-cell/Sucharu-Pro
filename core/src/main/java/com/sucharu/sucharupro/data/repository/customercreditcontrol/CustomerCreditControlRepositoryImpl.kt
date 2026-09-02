package com.sucharu.sucharupro.data.repository.customercreditcontrol

import com.sucharu.sucharupro.data.datasource.customercreditcontrol.CustomerCreditControlDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditControlAuditEvent
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditProfileEntity
import com.sucharu.sucharupro.domain.repository.customercreditcontrol.CustomerCreditControlRepository

/**
 * Concrete Repository implementation for Customer Credit Profiles (Module 14 Step 07).
 */
class CustomerCreditControlRepositoryImpl(
    private val dataSource: CustomerCreditControlDataSource
) : CustomerCreditControlRepository {

    override suspend fun saveProfile(profile: CustomerCreditProfileEntity): DomainResult<CustomerCreditProfileEntity> {
        return dataSource.saveProfile(profile)
    }

    override suspend fun getProfileByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditProfileEntity?> {
        return dataSource.getProfileByCustomerId(tenantId, projectId, customerId)
    }

    override suspend fun listProfiles(
        tenantId: String,
        projectId: String,
        financialHold: Boolean?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditProfileEntity>> {
        return dataSource.listProfiles(tenantId, projectId, financialHold, limit, offset)
    }

    override suspend fun recordAuditEvent(event: CustomerCreditControlAuditEvent): DomainResult<CustomerCreditControlAuditEvent> {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerCreditControlAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, customerId)
    }
}
