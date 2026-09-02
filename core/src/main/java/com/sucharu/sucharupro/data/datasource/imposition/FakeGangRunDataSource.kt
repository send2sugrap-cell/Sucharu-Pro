package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.GangRunSpecification
import com.sucharu.sucharupro.domain.model.imposition.GangRunStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe In-Memory Fake Data Source for Gang-Run Specifications.
 * Module 18 Step 02.
 */
class FakeGangRunDataSource : GangRunDataSource {

    private val storage = ConcurrentHashMap<String, GangRunSpecification>()

    override suspend fun saveGangRunSpecification(specification: GangRunSpecification): GangRunSpecification {
        val key = makeKey(specification.tenantId, specification.gangRunId)
        storage[key] = specification
        return specification
    }

    override suspend fun getGangRunSpecification(tenantId: String, gangRunId: String): GangRunSpecification? {
        val key = makeKey(tenantId, gangRunId)
        return storage[key]
    }

    override suspend fun listGangRunSpecifications(
        tenantId: String,
        limit: Int,
        offset: Int
    ): List<GangRunSpecification> {
        return storage.values
            .filter { it.tenantId == tenantId }
            .sortedByDescending { it.createdAt }
            .drop(offset)
            .take(limit)
    }

    override suspend fun updateGangRunStatus(
        tenantId: String,
        gangRunId: String,
        status: GangRunStatus,
        actor: String,
        notes: String?
    ): Boolean {
        val key = makeKey(tenantId, gangRunId)
        val existing = storage[key] ?: return false
        val updated = existing.copy(
            status = status,
            notes = notes ?: existing.notes
        )
        storage[key] = updated
        return true
    }

    fun clear() {
        storage.clear()
    }

    private fun makeKey(tenantId: String, id: String): String = "$tenantId:$id"
}
