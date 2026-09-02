package com.sucharu.sucharupro.data.datasource.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Data source interface for Commercial Commitments and conversion audit events.
 * Module 17 Step 03.
 */
interface CommercialCommitmentDataSource {

    suspend fun insertCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment>

    suspend fun updateCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment>

    suspend fun selectCommitmentById(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?>

    suspend fun selectCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?>

    suspend fun selectCommitmentByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<CommercialCommitment?>

    suspend fun listCommitments(tenantId: String, limit: Int = 50): DomainResult<List<CommercialCommitment>>

    suspend fun insertEvent(event: CommercialCommitmentEvent): DomainResult<CommercialCommitmentEvent>

    suspend fun listEventsByCommitmentId(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>>
}
