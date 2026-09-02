package com.sucharu.sucharupro.domain.repository.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateAllocationSource
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservation
import com.sucharu.sucharupro.domain.model.substratereservation.SubstrateReservationAuditEvent

interface SubstrateReservationRepository {
    suspend fun saveReservation(reservation: SubstrateReservation): SubstrateReservation
    suspend fun getReservationById(tenantId: String, reservationId: String): SubstrateReservation?
    suspend fun getReservationByIdempotencyKey(tenantId: String, idempotencyKey: String): SubstrateReservation?
    suspend fun listReservationsByOrder(tenantId: String, orderId: String): List<SubstrateReservation>
    suspend fun listReservationsByJob(tenantId: String, executionJobId: String): List<SubstrateReservation>
    suspend fun listReservationsBySku(tenantId: String, sku: String): List<SubstrateReservation>
    suspend fun listAllReservations(tenantId: String, limit: Int = 50): List<SubstrateReservation>
    suspend fun saveAuditEvent(event: SubstrateReservationAuditEvent)
    suspend fun listAuditEventsByReservation(tenantId: String, reservationId: String): List<SubstrateReservationAuditEvent>

    // Step 02: Physical Allocation Sources
    suspend fun saveAllocationSource(source: SubstrateAllocationSource): SubstrateAllocationSource
    suspend fun listAllocationsByReservation(tenantId: String, reservationId: String): List<SubstrateAllocationSource>
    suspend fun deleteAllocationsByReservation(tenantId: String, reservationId: String)
}
