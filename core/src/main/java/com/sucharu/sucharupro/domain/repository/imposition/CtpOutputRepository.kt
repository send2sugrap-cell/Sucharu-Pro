package com.sucharu.sucharupro.domain.repository.imposition

import com.sucharu.sucharupro.domain.model.imposition.CtpOutputSpecification

/**
 * Domain Repository interface for CTP Prepress Output Specifications.
 * Module 18 Step 05.
 */
interface CtpOutputRepository {
    suspend fun save(specification: CtpOutputSpecification): CtpOutputSpecification
    suspend fun findById(tenantId: String, ctpOutputId: String): CtpOutputSpecification?
    suspend fun findByJobId(tenantId: String, jobId: String): List<CtpOutputSpecification>
    suspend fun findBySourceImpositionId(tenantId: String, sourceImpositionId: String): List<CtpOutputSpecification>
    suspend fun listAll(tenantId: String): List<CtpOutputSpecification>
    suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: String,
        actor: String,
        reason: String? = null
    ): CtpOutputSpecification?
}
