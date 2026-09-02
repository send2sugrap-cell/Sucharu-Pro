package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.ImpositionDataSource
import com.sucharu.sucharupro.domain.model.imposition.ImpositionSpecification
import com.sucharu.sucharupro.domain.repository.imposition.ImpositionRepository

class ImpositionRepositoryImpl(
    private val dataSource: ImpositionDataSource
) : ImpositionRepository {

    override suspend fun saveSpecification(spec: ImpositionSpecification): ImpositionSpecification {
        return dataSource.saveSpecification(spec)
    }

    override suspend fun getSpecificationById(tenantId: String, impositionId: String): ImpositionSpecification? {
        return dataSource.getSpecificationById(tenantId, impositionId)
    }

    override suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<ImpositionSpecification> {
        return dataSource.listSpecificationsByJob(tenantId, jobId)
    }

    override suspend fun listSpecificationsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification> {
        return dataSource.listSpecificationsByOrder(tenantId, orderId)
    }

    override suspend fun listAllSpecifications(tenantId: String, limit: Int): List<ImpositionSpecification> {
        return dataSource.listAllSpecifications(tenantId, limit)
    }

    override suspend fun updateStatus(
        tenantId: String,
        impositionId: String,
        status: String,
        actor: String,
        notes: String?
    ): Boolean {
        return dataSource.updateStatus(tenantId, impositionId, status, actor, notes)
    }
}
