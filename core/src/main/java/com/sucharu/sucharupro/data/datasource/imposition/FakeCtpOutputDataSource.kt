package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.CtpOutputSpecification
import com.sucharu.sucharupro.domain.model.imposition.CtpOutputStatus
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-Memory fake implementation of [CtpOutputDataSource] for testing and mocking.
 * Module 18 Step 05.
 */
class FakeCtpOutputDataSource : CtpOutputDataSource {

    private val storage = ConcurrentHashMap<String, CtpOutputSpecification>()

    private fun storageKey(tenantId: String, ctpOutputId: String): String = "$tenantId:$ctpOutputId"

    override suspend fun save(specification: CtpOutputSpecification): CtpOutputSpecification {
        val key = storageKey(specification.tenantId, specification.ctpOutputId)
        storage[key] = specification
        return specification
    }

    override suspend fun findById(tenantId: String, ctpOutputId: String): CtpOutputSpecification? {
        return storage[storageKey(tenantId, ctpOutputId)]
    }

    override suspend fun findByJobId(tenantId: String, jobId: String): List<CtpOutputSpecification> {
        return storage.values.filter { it.tenantId == tenantId && it.jobId == jobId }
    }

    override suspend fun findBySourceImpositionId(tenantId: String, sourceImpositionId: String): List<CtpOutputSpecification> {
        return storage.values.filter { it.tenantId == tenantId && it.sourceImpositionId == sourceImpositionId }
    }

    override suspend fun listAll(tenantId: String): List<CtpOutputSpecification> {
        return storage.values.filter { it.tenantId == tenantId }
    }

    override suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: String,
        actor: String,
        reason: String?
    ): CtpOutputSpecification? {
        val existing = findById(tenantId, ctpOutputId) ?: return null
        val updated = existing.copy(
            status = CtpOutputStatus.valueOf(newStatus),
            notes = if (reason != null) "${existing.notes ?: ""}\nStatus changed to $newStatus by $actor: $reason".trim() else existing.notes,
            updatedAt = Instant.now()
        )
        storage[storageKey(tenantId, ctpOutputId)] = updated
        return updated
    }

    fun clear() {
        storage.clear()
    }
}
