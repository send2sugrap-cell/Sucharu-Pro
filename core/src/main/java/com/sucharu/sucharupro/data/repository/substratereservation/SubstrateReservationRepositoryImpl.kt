package com.sucharu.sucharupro.data.repository.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.SubstrateReservationDataSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateAllocationSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservation
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationAuditEvent
import com.sucharu.sucharupro.domain.repository.substratereservation.SubstrateReservationRepository

class SubstrateReservationRepositoryImpl(
    private val dataSource: SubstrateReservationDataSource
) : SubstrateReservationRepository {

    override suspend fun saveReservation(reservation: SubstrateReservation): SubstrateReservation {
        return dataSource.saveReservation(reservation)
    }

    override suspend fun getReservationById(tenantId: String, reservationId: String): SubstrateReservation? {
        val res = dataSource.getReservationById(tenantId, reservationId) ?: return null
        val allocations = dataSource.listAllocationsByReservation(tenantId, reservationId)
        return res.copy(allocationSources = allocations)
    }

    override suspend fun getReservationByIdempotencyKey(tenantId: String, idempotencyKey: String): SubstrateReservation? {
        val res = dataSource.getReservationByIdempotencyKey(tenantId, idempotencyKey) ?: return null
        val allocations = dataSource.listAllocationsByReservation(tenantId, res.reservationId)
        return res.copy(allocationSources = allocations)
    }

    override suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation> {
        val list = dataSource.listReservationsByOrder(tenantId, orderId)
        return list.map { res ->
            val allocations = dataSource.listAllocationsByReservation(tenantId, res.reservationId)
            res.copy(allocationSources = allocations)
        }
    }

    override suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation> {
        val list = dataSource.listReservationsByJob(tenantId, executionJobId)
        return list.map { res ->
            val allocations = dataSource.listAllocationsByReservation(tenantId, res.reservationId)
            res.copy(allocationSources = allocations)
        }
    }

    override suspend fun listReservationsBySku(tenantId: String, sku: String): List<SubstrateReservation> {
        val list = dataSource.listReservationsBySku(tenantId, sku)
        return list.map { res ->
            val allocations = dataSource.listAllocationsByReservation(tenantId, res.reservationId)
            res.copy(allocationSources = allocations)
        }
    }

    override suspend fun listAllReservations(tenantId: String, limit: Int): List<SubstrateReservation> {
        val list = dataSource.listAllReservations(tenantId, limit)
        return list.map { res ->
            val allocations = dataSource.listAllocationsByReservation(tenantId, res.reservationId)
            res.copy(allocationSources = allocations)
        }
    }

    override suspend fun saveAuditEvent(event: SubstrateReservationAuditEvent) {
        dataSource.saveAuditEvent(event)
    }

    override suspend fun listAuditEventsByReservation(tenantId: String, reservationId: String): List<SubstrateReservationAuditEvent> {
        return dataSource.listAuditEventsByReservation(tenantId, reservationId)
    }

    override suspend fun saveAllocationSource(source: SubstrateAllocationSource): SubstrateAllocationSource {
        return dataSource.saveAllocationSource(source)
    }

    override suspend fun listAllocationsByReservation(tenantId: String, reservationId: String): List<SubstrateAllocationSource> {
        return dataSource.listAllocationsByReservation(tenantId, reservationId)
    }

    override suspend fun deleteAllocationsByReservation(tenantId: String, reservationId: String) {
        dataSource.deleteAllocationsByReservation(tenantId, reservationId)
    }
}
