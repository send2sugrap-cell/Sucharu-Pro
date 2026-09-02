package com.sucharu.sucharupro.data.datasource.commercialcommitment

import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitment
import com.sucharu.sucharupro.domain.model.commercialcommitment.CommercialCommitmentEvent
import com.sucharu.sucharupro.domain.model.common.DomainResult
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory, thread-safe DataSource for Commercial Commitments.
 * Used for fast unit and service test suites.
 * Module 17 Step 03.
 */
class FakeCommercialCommitmentDataSource : CommercialCommitmentDataSource {

    private val commitments = ConcurrentHashMap<String, CommercialCommitment>()
    private val events = ConcurrentHashMap<String, CommercialCommitmentEvent>()

    override suspend fun insertCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> {
        commitments[commitment.commitmentId] = commitment
        return DomainResult.Success(commitment)
    }

    override suspend fun updateCommitment(commitment: CommercialCommitment): DomainResult<CommercialCommitment> {
        commitments[commitment.commitmentId] = commitment
        return DomainResult.Success(commitment)
    }

    override suspend fun selectCommitmentById(tenantId: String, commitmentId: String): DomainResult<CommercialCommitment?> {
        val found = commitments[commitmentId]?.takeIf { it.tenantId == tenantId }
        return DomainResult.Success(found)
    }

    override suspend fun selectCommitmentByQuotation(tenantId: String, quotationId: String): DomainResult<CommercialCommitment?> {
        val found = commitments.values.find { it.tenantId == tenantId && it.quotationId == quotationId }
        return DomainResult.Success(found)
    }

    override suspend fun selectCommitmentByIdempotencyKey(tenantId: String, idempotencyKey: String): DomainResult<CommercialCommitment?> {
        val found = commitments.values.find { it.tenantId == tenantId && it.idempotencyKey == idempotencyKey }
        return DomainResult.Success(found)
    }

    override suspend fun listCommitments(tenantId: String, limit: Int): DomainResult<List<CommercialCommitment>> {
        val list = commitments.values
            .filter { it.tenantId == tenantId }
            .sortedByDescending { it.createdAt }
            .take(limit)
        return DomainResult.Success(list)
    }

    override suspend fun insertEvent(event: CommercialCommitmentEvent): DomainResult<CommercialCommitmentEvent> {
        events[event.eventId] = event
        return DomainResult.Success(event)
    }

    override suspend fun listEventsByCommitmentId(tenantId: String, commitmentId: String): DomainResult<List<CommercialCommitmentEvent>> {
        val list = events.values
            .filter { it.tenantId == tenantId && it.commitmentId == commitmentId }
            .sortedBy { it.occurredAt }
        return DomainResult.Success(list)
    }
}
