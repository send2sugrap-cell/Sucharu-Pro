package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.SignatureImpositionSpecification
import com.sucharu.sucharupro.domain.model.imposition.SignatureStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe, tenant-isolated in-memory Fake DataSource for Signature Imposition.
 * Module 18 Step 04.
 */
class FakeSignatureImpositionDataSource : SignatureImpositionDataSource {

    // Key: tenantId -> (signatureImpositionId -> Specification)
    private val store = ConcurrentHashMap<String, ConcurrentHashMap<String, SignatureImpositionSpecification>>()

    override suspend fun saveSpecification(
        tenantId: String,
        specification: SignatureImpositionSpecification
    ): SignatureImpositionSpecification {
        val tenantMap = store.computeIfAbsent(tenantId) { ConcurrentHashMap() }
        tenantMap[specification.signatureImpositionId] = specification
        return specification
    }

    override suspend fun getSpecificationById(
        tenantId: String,
        signatureImpositionId: String
    ): SignatureImpositionSpecification? {
        return store[tenantId]?.get(signatureImpositionId)
    }

    override suspend fun listSpecifications(tenantId: String): List<SignatureImpositionSpecification> {
        return store[tenantId]?.values?.sortedByDescending { it.createdAt } ?: emptyList()
    }

    override suspend fun listSpecificationsByJob(
        tenantId: String,
        jobId: String
    ): List<SignatureImpositionSpecification> {
        return store[tenantId]?.values
            ?.filter { it.jobId == jobId }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    override suspend fun updateStatus(
        tenantId: String,
        signatureImpositionId: String,
        status: SignatureStatus,
        actor: String,
        notes: String?
    ): Boolean {
        val tenantMap = store[tenantId] ?: return false
        val current = tenantMap[signatureImpositionId] ?: return false
        tenantMap[signatureImpositionId] = current.copy(status = status, notes = notes ?: current.notes)
        return true
    }
}
