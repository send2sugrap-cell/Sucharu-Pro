package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.ImpositionSpecification
import com.sucharu.sucharupro.domain.model.imposition.ImpositionStatus
import java.util.concurrent.ConcurrentHashMap

class FakeImpositionDataSource : ImpositionDataSource {

    private val storage = ConcurrentHashMap<String, ImpositionSpecification>()

    override suspend fun saveSpecification(spec: ImpositionSpecification): ImpositionSpecification {
        val key = "${spec.tenantId}:${spec.impositionId}"
        storage[key] = spec
        return spec
    }

    override suspend fun getSpecificationById(tenantId: String, impositionId: String): ImpositionSpecification? {
        val key = "$tenantId:$impositionId"
        return storage[key]
    }

    override suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<ImpositionSpecification> {
        return storage.values.filter { it.tenantId == tenantId && it.jobId == jobId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun listSpecificationsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification> {
        return storage.values.filter { it.tenantId == tenantId && it.orderId == orderId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun listAllSpecifications(tenantId: String, limit: Int): List<ImpositionSpecification> {
        return storage.values.filter { it.tenantId == tenantId }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    override suspend fun updateStatus(
        tenantId: String,
        impositionId: String,
        status: String,
        actor: String,
        notes: String?
    ): Boolean {
        val key = "$tenantId:$impositionId"
        val existing = storage[key] ?: return false
        val newStatus = try { ImpositionStatus.valueOf(status) } catch (e: Exception) { ImpositionStatus.OPTIMIZED }
        val updated = existing.copy(status = newStatus, notes = notes ?: existing.notes)
        storage[key] = updated
        return true
    }
}
