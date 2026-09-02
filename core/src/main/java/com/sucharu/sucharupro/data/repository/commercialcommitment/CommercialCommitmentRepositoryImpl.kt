package com.sucharu.sucharupro.data.repository.commercialcommitment

import com.sucharu.sucharupro.data.datasource.commercialcommitment.CommercialCommitmentDataSource
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.commercialcommitment.CommercialCommitmentRepository

/**
 * Implementation of [CommercialCommitmentRepository] backed by [CommercialCommitmentDataSource].
 * Module 17 Step 03.
 */
class CommercialCommitmentRepositoryImpl(
    private val dataSource: CommercialCommitmentDataSource
) : CommercialCommitmentRepository {

    override suspend fun saveCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> =
        dataSource.insertCommitment(commitment)

    override suspend fun updateCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> =
        dataSource.updateCommitment(commitment)

    override suspend fun findCommitmentById(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?> =
        dataSource.selectCommitmentById(tenantId, commitmentId)

    override suspend fun findCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?> =
        dataSource.selectCommitmentByQuotation(tenantId, quotationId)

    override suspend fun findCommitmentByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<CommercialCommitment?> =
        dataSource.selectCommitmentByIdempotencyKey(tenantId, idempotencyKey)

    override suspend fun listCommitments(tenantId: String, limit: Int): DomainResult<List<CommercialCommitment>> =
        dataSource.listCommitments(tenantId, limit)

    override suspend fun saveEvent(event: CommercialCommitmentEvent): DomainResult<CommercialCommitmentEvent> =
        dataSource.insertEvent(event)

    override suspend fun listEvents(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>> =
        dataSource.listEventsByCommitmentId(tenantId, commitmentId)
}
