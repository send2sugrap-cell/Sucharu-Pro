package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateEnterpriseAuditDataSource
import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReservationDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateEnterpriseAuditRepositoryImpl
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReservationRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateEnterpriseAuditServiceTest {

    private lateinit var auditDataSource: FakeSubstrateEnterpriseAuditDataSource
    private lateinit var reservationDataSource: FakeSubstrateReservationDataSource
    private lateinit var auditRepository: SubstrateEnterpriseAuditRepositoryImpl
    private lateinit var reservationRepository: SubstrateReservationRepositoryImpl
    private lateinit var auditService: SubstrateEnterpriseAuditServiceImpl

    private val tenantId = "TENANT-001"
    private val reservationId = "RES-SRV-01"

    @Before
    fun setUp() {
        runBlocking {
            auditDataSource = FakeSubstrateEnterpriseAuditDataSource()
            reservationDataSource = FakeSubstrateReservationDataSource()
            auditRepository = SubstrateEnterpriseAuditRepositoryImpl(auditDataSource)
            reservationRepository = SubstrateReservationRepositoryImpl(reservationDataSource)

            auditService = SubstrateEnterpriseAuditServiceImpl(
                auditRepository = auditRepository,
                reservationRepository = reservationRepository
            )

            // Seed a sample reservation
            val reservation = SubstrateReservation(
                reservationId = reservationId,
                tenantId = tenantId,
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                executionJobId = "JOB-01",
                workOrderId = null,
                productId = "PROD-01",
                sku = "ART-300",
                productName = "Art Card 300 GSM",
                warehouseId = "WH-01",
                locationId = null,
                stockType = PaperStockType.ART_CARD,
                gsm = BigDecimal("300.0000"),
                sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
                reservedSheets = 5000L,
                reservedReams = BigDecimal("10.0000"),
                reservedWeightKg = BigDecimal("200.0000"),
                status = SubstrateReservationStatus.ALLOCATED_HARD,
                mode = SubstrateReservationMode.HARD,
                idempotencyKey = "IDEMP-01",
                expiryTimestamp = null,
                softHoldExpiresAt = null,
                promotedAt = null,
                promotedBy = null,
                reservedBy = "test_user",
                reservedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                notes = null,
                allocationSources = emptyList()
            )
            reservationRepository.saveReservation(reservation)
        }
    }

    @Test
    fun testRecordAndRetrieveAuditTrail() {
        runBlocking {
            val event1 = auditService.recordAuditEvent(
                tenantId = tenantId,
                reservationId = reservationId,
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                eventType = ReservationAuditEventType.REQUIREMENT_RESOLVED,
                previousState = null,
                newState = "RESOLVED",
                actorType = AuditActorType.USER,
                actorId = "usr-1",
                role = "PLANNER",
                permissionContext = "TEST",
                reason = "Requirement calculated",
                correlationId = "corr-1",
                traceId = null,
                idempotencyKey = null,
                sourceOperation = "RESOLVE"
            )

            val event2 = auditService.recordAuditEvent(
                tenantId = tenantId,
                reservationId = reservationId,
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                eventType = ReservationAuditEventType.HARD_ALLOCATED,
                previousState = "RESOLVED",
                newState = "ALLOCATED_HARD",
                actorType = AuditActorType.USER,
                actorId = "usr-1",
                role = "PLANNER",
                permissionContext = "TEST",
                reason = "Allocated to job",
                correlationId = "corr-2",
                traceId = null,
                idempotencyKey = null,
                sourceOperation = "ALLOCATE"
            )

            val history = auditService.getAuditHistory(tenantId, reservationId)
            assertEquals(2, history.size)
            assertEquals(event1.chainHash, history[1].previousAuditHash)
            assertEquals(event2.chainHash, history[1].chainHash)
        }
    }

    @Test
    fun testReconciliationAndIntegrityVerification() {
        runBlocking {
            // Record an audit event
            auditService.recordAuditEvent(
                tenantId = tenantId,
                reservationId = reservationId,
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                eventType = ReservationAuditEventType.HARD_ALLOCATED,
                previousState = null,
                newState = "ALLOCATED_HARD",
                actorType = AuditActorType.USER,
                actorId = "usr-1",
                role = "PLANNER",
                permissionContext = "TEST",
                reason = "Hard hold",
                correlationId = "corr-1",
                traceId = null,
                idempotencyKey = null,
                sourceOperation = "ALLOCATE"
            )

            val reconciliation = auditService.reconcileReservation(
                tenantId = tenantId,
                reservationId = reservationId,
                actor = "auditor"
            )

            assertEquals(ReconciliationStatus.HEALTHY, reconciliation.status)

            val integrityResult = auditService.verifyReservationIntegrity(
                tenantId = tenantId,
                reservationId = reservationId,
                actor = "security_officer"
            )

            assertTrue(integrityResult.isValidChain)
            assertEquals(IntegrityVerificationStatus.INTACT, integrityResult.status)
        }
    }

    @Test
    fun testGenerateAiHandoffContract() {
        runBlocking {
            val handoff = auditService.generateAiHandoffContract(
                tenantId = tenantId,
                reservationId = reservationId,
                actor = "ai_controller"
            )

            assertEquals("6.0.0", handoff.contractVersion)
            assertEquals(reservationId, handoff.reservationId)
            assertTrue(handoff.isReadOnly)
            assertNotNull(handoff.masterIntegrityHash)

            val savedPayload = auditRepository.getLatestAiHandoffSnapshot(tenantId, reservationId)
            assertNotNull(savedPayload)
            assertTrue(savedPayload!!.contains("\"contractVersion\": \"6.0.0\""))
        }
    }
}
