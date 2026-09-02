package com.sucharu.sucharupro.data.datasource.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateAllocationSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservation
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationAuditEvent
import java.util.concurrent.ConcurrentHashMap

class FakeSubstrateReservationDataSource : SubstrateReservationDataSource {

    private val reservations = ConcurrentHashMap<String, SubstrateReservation>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<SubstrateReservationAuditEvent>>()
    private val allocations = ConcurrentHashMap<String, MutableList<SubstrateAllocationSource>>()

    override suspend fun saveReservation(reservation: SubstrateReservation): SubstrateReservation {
        reservations[reservation.reservationId] = reservation
        return reservation
    }

    override suspend fun getReservationById(tenantId: String, reservationId: String): SubstrateReservation? {
        val res = reservations[reservationId]
        return if (res?.tenantId == tenantId) res else null
    }

    override suspend fun getReservationByIdempotencyKey(tenantId: String, idempotencyKey: String): SubstrateReservation? {
        return reservations.values.firstOrNull { it.tenantId == tenantId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation> {
        return reservations.values.filter { it.tenantId == tenantId && it.orderId == orderId }
    }

    override suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation> {
        return reservations.values.filter { it.tenantId == tenantId && it.executionJobId == executionJobId }
    }

    override suspend fun listReservationsBySku(tenantId: String, sku: String): List<SubstrateReservation> {
        return reservations.values.filter { it.tenantId == tenantId && it.sku.equals(sku, ignoreCase = true) }
    }

    override suspend fun listAllReservations(tenantId: String, limit: Int): List<SubstrateReservation> {
        return reservations.values.filter { it.tenantId == tenantId }.take(limit)
    }

    override suspend fun saveAuditEvent(event: SubstrateReservationAuditEvent) {
        val list = auditEvents.computeIfAbsent(event.reservationId) { mutableListOf() }
        list.add(event)
    }

    override suspend fun listAuditEventsByReservation(tenantId: String, reservationId: String): List<SubstrateReservationAuditEvent> {
        return auditEvents[reservationId]?.filter { it.tenantId == tenantId } ?: emptyList()
    }

    override suspend fun saveAllocationSource(source: SubstrateAllocationSource): SubstrateAllocationSource {
        val list = allocations.computeIfAbsent(source.reservationId) { mutableListOf() }
        list.removeAll { it.allocationId == source.allocationId }
        list.add(source)
        return source
    }

    override suspend fun listAllocationsByReservation(tenantId: String, reservationId: String): List<SubstrateAllocationSource> {
        return allocations[reservationId]?.filter { it.tenantId == tenantId } ?: emptyList()
    }

    override suspend fun deleteAllocationsByReservation(tenantId: String, reservationId: String) {
        allocations[reservationId]?.removeAll { it.tenantId == tenantId }
    }

    fun clear() {
        reservations.clear()
        auditEvents.clear()
        allocations.clear()
    }
}
