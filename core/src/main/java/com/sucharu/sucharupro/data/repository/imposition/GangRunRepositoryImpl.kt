package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.GangRunDataSource
import com.sucharu.sucharupro.domain.model.imposition.GangRunSpecification
import com.sucharu.sucharupro.domain.model.imposition.GangRunStatus
import com.sucharu.sucharupro.domain.repository.imposition.GangRunRepository

/**
 * Implementation of GangRunRepository delegating to GangRunDataSource.
 * Module 18 Step 02.
 */
class GangRunRepositoryImpl(
    private val dataSource: GangRunDataSource
) : GangRunRepository {

    override suspend fun save(specification: GangRunSpecification): GangRunSpecification {
        return dataSource.saveGangRunSpecification(specification)
    }

    override suspend fun findById(tenantId: String, gangRunId: String): GangRunSpecification? {
        return dataSource.getGangRunSpecification(tenantId, gangRunId)
    }

    override suspend fun listAll(tenantId: String, limit: Int, offset: Int): List<GangRunSpecification> {
        return dataSource.listGangRunSpecifications(tenantId, limit, offset)
    }

    override suspend fun updateStatus(
        tenantId: String,
        gangRunId: String,
        status: GangRunStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return dataSource.updateGangRunStatus(tenantId, gangRunId, status, actor, notes)
    }
}
