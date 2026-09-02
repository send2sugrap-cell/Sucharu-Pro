package com.sucharu.sucharupro.data.repository.imposition

import com.sucharu.sucharupro.data.datasource.imposition.SignatureImpositionDataSource
import com.sucharu.sucharupro.domain.model.imposition.SignatureImpositionSpecification
import com.sucharu.sucharupro.domain.model.imposition.SignatureStatus
import com.sucharu.sucharupro.domain.repository.imposition.SignatureImpositionRepository

/**
 * Production implementation of SignatureImpositionRepository delegating to SignatureImpositionDataSource.
 * Module 18 Step 04.
 */
class SignatureImpositionRepositoryImpl(
    private val dataSource: SignatureImpositionDataSource
) : SignatureImpositionRepository {

    override suspend fun saveSpecification(
        tenantId: String,
        specification: SignatureImpositionSpecification
    ): SignatureImpositionSpecification {
        return dataSource.saveSpecification(tenantId, specification)
    }

    override suspend fun getSpecificationById(
        tenantId: String,
        signatureImpositionId: String
    ): SignatureImpositionSpecification? {
        return dataSource.getSpecificationById(tenantId, signatureImpositionId)
    }

    override suspend fun listSpecifications(tenantId: String): List<SignatureImpositionSpecification> {
        return dataSource.listSpecifications(tenantId)
    }

    override suspend fun listSpecificationsByJob(
        tenantId: String,
        jobId: String
    ): List<SignatureImpositionSpecification> {
        return dataSource.listSpecificationsByJob(tenantId, jobId)
    }

    override suspend fun updateStatus(
        tenantId: String,
        signatureImpositionId: String,
        status: SignatureStatus,
        actor: String,
        notes: String?
    ): Boolean {
        return dataSource.updateStatus(tenantId, signatureImpositionId, status, actor, notes)
    }
}
