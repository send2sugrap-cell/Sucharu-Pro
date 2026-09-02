package com.sucharu.sucharupro.domain.repository.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Domain Repository interface for Commercial Commitments.
 * Module 17 Step 03.
 */
interface CommercialCommitmentRepository {

    suspend fun saveCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment>

    suspend fun updateCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment>

    suspend fun findCommitmentById(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?>

    suspend fun findCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?>

    suspend fun findCommitmentByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<CommercialCommitment?>

    suspend fun listCommitments(tenantId: String, limit: Int = 50): DomainResult<List<CommercialCommitment>>

    suspend fun saveEvent(event: CommercialCommitmentEvent): DomainResult<CommercialCommitmentEvent>

    suspend fun listEvents(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>>
}
