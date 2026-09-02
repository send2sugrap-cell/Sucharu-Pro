package com.sucharu.sucharupro.domain.repository.imposition

import com.sucharu.sucharupro.domain.model.imposition.SignatureImpositionSpecification
import com.sucharu.sucharupro.domain.model.imposition.SignatureStatus

/**
 * Domain Repository Interface for Multi-Page Signature Imposition.
 * Module 18 Step 04.
 */
interface SignatureImpositionRepository {
    suspend fun saveSpecification(tenantId: String, specification: SignatureImpositionSpecification): SignatureImpositionSpecification
    suspend fun getSpecificationById(tenantId: String, signatureImpositionId: String): SignatureImpositionSpecification?
    suspend fun listSpecifications(tenantId: String): List<SignatureImpositionSpecification>
    suspend fun listSpecificationsByJob(tenantId: String, jobId: String): List<SignatureImpositionSpecification>
    suspend fun updateStatus(tenantId: String, signatureImpositionId: String, status: SignatureStatus, actor: String, notes: String? = null): Boolean
}
