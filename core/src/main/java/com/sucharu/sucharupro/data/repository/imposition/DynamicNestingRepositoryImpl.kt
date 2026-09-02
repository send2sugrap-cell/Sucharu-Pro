package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.DynamicNestingDataSource
import com.sucharu.sucharupro.domain.model.imposition.DynamicNestingSpecification
import com.sucharu.sucharupro.domain.model.imposition.NestingStatus
import com.sucharu.sucharupro.domain.repository.imposition.DynamicNestingRepository

/**
 * Repository implementation delegating to DynamicNestingDataSource.
 * Module 18 Step 03.
 */
class DynamicNestingRepositoryImpl(
    private val dataSource: DynamicNestingDataSource
) : DynamicNestingRepository {

    override suspend fun save(specification: DynamicNestingSpecification): DynamicNestingSpecification {
        return dataSource.saveNestingSpecification(specification)
    }

    override suspend fun findById(tenantId: String, nestingId: String): DynamicNestingSpecification? {
        return dataSource.getNestingSpecification(tenantId, nestingId)
    }

    override suspend fun listAll(tenantId: String, limit: Int, offset: Int): List<DynamicNestingSpecification> {
        return dataSource.listNestingSpecifications(tenantId, limit, offset)
    }

    override suspend fun updateStatus(
        tenantId: String,
        nestingId: String,
        status: NestingStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return dataSource.updateNestingStatus(tenantId, nestingId, status, actor, notes)
    }
}
