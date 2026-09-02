package com.sucharu.sucharupro.data.datasource.customercreditcontrol

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditControlAuditEvent
import com.sucharu.sucharupro.domain.model.customercreditcontrol.CustomerCreditProfileEntity
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake DataSource for Customer Credit Profiles (Module 14 Step 07).
 */
class FakeCustomerCreditControlDataSource : CustomerCreditControlDataSource {

    private val profiles = ConcurrentHashMap<String, CustomerCreditProfileEntity>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<CustomerCreditControlAuditEvent>>()

    private fun key(tenantId: String, projectId: String, customerId: String) = "$tenantId:$projectId:$customerId"

    override suspend fun saveProfile(profile: CustomerCreditProfileEntity): DomainResult<CustomerCreditProfileEntity> {
        val k = key(profile.tenantId, profile.projectId, profile.customerId)
        val existing = profiles[k]
        if (existing != null && existing.version != profile.version - 1 && profile.version > 1) {
            // Version check
        }
        profiles[k] = profile
        return DomainResult.Success(profile)
    }

    override suspend fun getProfileByCustomerId(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<CustomerCreditProfileEntity?> {
        val k = key(tenantId, projectId, customerId)
        return DomainResult.Success(profiles[k])
    }

    override suspend fun listProfiles(
        tenantId: String,
        projectId: String,
        financialHold: Boolean?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCreditProfileEntity>> {
        var list = profiles.values.filter { it.tenantId == tenantId && it.projectId == projectId }
        if (financialHold != null) {
            list = list.filter { it.financialHold == financialHold }
        }
        return DomainResult.Success(list.drop(offset).take(limit))
    }

    override suspend fun recordAuditEvent(event: CustomerCreditControlAuditEvent): DomainResult<CustomerCreditControlAuditEvent> {
        val k = key(event.tenantId, event.projectId, event.customerId)
        auditEvents.computeIfAbsent(k) { mutableListOf() }.add(event)
        return DomainResult.Success(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String
    ): DomainResult<List<CustomerCreditControlAuditEvent>> {
        val k = key(tenantId, projectId, customerId)
        val list = auditEvents[k]?.sortedByDescending { it.occurredAt } ?: emptyList()
        return DomainResult.Success(list)
    }
}
