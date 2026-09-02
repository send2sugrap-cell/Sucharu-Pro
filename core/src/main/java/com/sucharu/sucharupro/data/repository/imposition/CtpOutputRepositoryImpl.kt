package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.CtpOutputDataSource
import com.sucharu.sucharupro.domain.model.imposition.CtpOutputSpecification
import com.sucharu.sucharupro.domain.repository.imposition.CtpOutputRepository

/**
 * Production implementation of [CtpOutputRepository] delegating to [CtpOutputDataSource].
 * Module 18 Step 05.
 */
class CtpOutputRepositoryImpl(
    private val dataSource: CtpOutputDataSource
) : CtpOutputRepository {

    override suspend fun save(specification: CtpOutputSpecification): CtpOutputSpecification {
        return dataSource.save(specification)
    }

    override suspend fun findById(tenantId: String, ctpOutputId: String): CtpOutputSpecification? {
        return dataSource.findById(tenantId, ctpOutputId)
    }

    override suspend fun findByJobId(tenantId: String, jobId: String): List<CtpOutputSpecification> {
        return dataSource.findByJobId(tenantId, jobId)
    }

    override suspend fun findBySourceImpositionId(tenantId: String, sourceImpositionId: String): List<CtpOutputSpecification> {
        return dataSource.findBySourceImpositionId(tenantId, sourceImpositionId)
    }

    override suspend fun listAll(tenantId: String): List<CtpOutputSpecification> {
        return dataSource.listAll(tenantId)
    }

    override suspend fun updateStatus(
        tenantId: String,
        ctpOutputId: String,
        newStatus: String,
        actor: String,
        reason: String?
    ): CtpOutputSpecification? {
        return dataSource.updateStatus(tenantId, ctpOutputId, newStatus, actor, reason)
    }
}
