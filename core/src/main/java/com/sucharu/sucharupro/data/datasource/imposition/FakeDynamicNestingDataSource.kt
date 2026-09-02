package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.DynamicNestingSpecification
import com.sucharu.sucharupro.domain.model.imposition.NestingStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory Mock Data Source for Dynamic Nesting (unit/integration testing & sandbox).
 * Module 18 Step 03.
 */
class FakeDynamicNestingDataSource : DynamicNestingDataSource {

    private val storage = ConcurrentHashMap<String, MutableMap<String, DynamicNestingSpecification>>()

    override suspend fun saveNestingSpecification(specification: DynamicNestingSpecification): DynamicNestingSpecification {
        val tenantMap = storage.computeIfAbsent(specification.tenantId) { ConcurrentHashMap() }
        tenantMap[specification.nestingId] = specification
        return specification
    }

    override suspend fun getNestingSpecification(tenantId: String, nestingId: String): DynamicNestingSpecification? {
        return storage[tenantId]?.get(nestingId)
    }

    override suspend fun listNestingSpecifications(
        tenantId: String,
        limit: Int,
        offset: Int
    ): List<DynamicNestingSpecification> {
        val tenantMap = storage[tenantId] ?: return emptyList()
        return tenantMap.values
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun updateNestingStatus(
        tenantId: String,
        nestingId: String,
        status: NestingStatus,
        actor: String,
        notes: String?
    ): Boolean {
        val tenantMap = storage[tenantId] ?: return false
        val existing = tenantMap[nestingId] ?: return false
        tenantMap[nestingId] = existing.copy(
            status = status,
            notes = notes ?: existing.notes,
            version = existing.version + 1
        )
        return true
    }
}
