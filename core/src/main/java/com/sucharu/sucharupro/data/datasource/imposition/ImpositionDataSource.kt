package com.sucharu.sucharupro.data.datasource.imposition

import com.sucharu.sucharupro.domain.model.imposition.ImpositionSpecification

interface ImpositionDataSource {
    suspend fun saveSpecification(spec: ImpositionSpecification): ImpositionSpecification
    suspend fun getSpecificationById(tenantId: String, impositionId: String): ImpositionSpecification?
    suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<ImpositionSpecification>
    suspend fun listSpecificationsByOrder(tenantId: String, orderId: String): List<ImpositionSpecification>
    suspend fun listAllSpecifications(tenantId: String, limit: Int = 50): List<ImpositionSpecification>
    suspend fun updateStatus(tenantId: String, impositionId: String, status: String, actor: String, notes: String?): Boolean
}
