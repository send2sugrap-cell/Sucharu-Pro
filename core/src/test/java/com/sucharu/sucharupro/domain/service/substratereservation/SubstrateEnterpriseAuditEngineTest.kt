package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class SubstrateEnterpriseAuditEngineTest {

    @Test
    fun testComputeRecordHash_isDeterministic() {
        val hash1 = SubstrateEnterpriseAuditEngine.computeRecordHash(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            reservationVersion = 1L,
            jobId = "JOB-01",
            orderId = "ORD-01",
            orderItemId = "ITEM-01",
            eventType = ReservationAuditEventType.HARD_ALLOCATED,
            previousState = "RESERVED_SOFT",
            newState = "ALLOCATED_HARD",
            actorType = AuditActorType.USER,
            actorId = "USR-01",
            role = "MANAGER",
            timestamp = 1756880000000L,
            correlationId = "CORR-01",
            sourceOperation = "ALLOCATE_HARD"
        )

        val hash2 = SubstrateEnterpriseAuditEngine.computeRecordHash(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            reservationVersion = 1L,
            jobId = "JOB-01",
            orderId = "ORD-01",
            orderItemId = "ITEM-01",
            eventType = ReservationAuditEventType.HARD_ALLOCATED,
            previousState = "RESERVED_SOFT",
            newState = "ALLOCATED_HARD",
            actorType = AuditActorType.USER,
            actorId = "USR-01",
            role = "MANAGER",
            timestamp = 1756880000000L,
            correlationId = "CORR-01",
            sourceOperation = "ALLOCATE_HARD"
        )

        assertEquals(hash1, hash2)
        assertEquals(64, hash1.length)
    }

    @Test
    fun testVerifyAuditChain_intactChain() {
        val records = mutableListOf<SubstrateEnterpriseAuditRecord>()
        var prevHash: String? = null

        val events = listOf(
            ReservationAuditEventType.REQUIREMENT_RESOLVED,
            ReservationAuditEventType.INVENTORY_INTERLOCKED,
            ReservationAuditEventType.SOFT_RESERVED,
            ReservationAuditEventType.HARD_ALLOCATED
        )

        var time = 1756880000000L
        for (event in events) {
            val recHash = SubstrateEnterpriseAuditEngine.computeRecordHash(
                tenantId = "TENANT-001",
                reservationId = "RES-01",
                reservationVersion = 1L,
                jobId = "JOB-01",
                orderId = "ORD-01",
                orderItemId = "ITEM-01",
                eventType = event,
                previousState = null,
                newState = event.name,
                actorType = AuditActorType.SYSTEM,
                actorId = "sys",
                role = "SYSTEM",
                timestamp = time,
                correlationId = "corr",
                sourceOperation = "OP"
            )
            val chain = SubstrateEnterpriseAuditEngine.computeChainHash(prevHash, recHash)

            records.add(
                SubstrateEnterpriseAuditRecord(
                    auditId = UUID.randomUUID().toString(),
                    tenantId = "TENANT-001",
                    reservationId = "RES-01",
                    reservationVersion = 1L,
                    jobId = "JOB-01",
                    orderId = "ORD-01",
                    orderItemId = "ITEM-01",
                    eventType = event,
                    previousState = null,
                    newState = event.name,
                    actorType = AuditActorType.SYSTEM,
                    actorId = "sys",
                    role = "SYSTEM",
                    permissionContext = "TEST",
                    timestamp = time,
                    reason = "test reason",
                    correlationId = "corr",
                    sourceOperation = "OP",
                    recordHash = recHash,
                    previousAuditHash = prevHash,
                    chainHash = chain
                )
            )
            prevHash = chain
            time += 1000L
        }

        val result = SubstrateEnterpriseAuditEngine.verifyAuditChain(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            records = records,
            verifiedBy = "auditor"
        )

        assertTrue(result.isValidChain)
        assertEquals(IntegrityVerificationStatus.INTACT, result.status)
        assertTrue(result.tamperedRecordIds.isEmpty())
    }

    @Test
    fun testVerifyAuditChain_detectsTamperedRecord() {
        val records = mutableListOf<SubstrateEnterpriseAuditRecord>()
        var prevHash: String? = null

        val recHash = SubstrateEnterpriseAuditEngine.computeRecordHash(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            reservationVersion = 1L,
            jobId = "JOB-01",
            orderId = "ORD-01",
            orderItemId = "ITEM-01",
            eventType = ReservationAuditEventType.HARD_ALLOCATED,
            previousState = null,
            newState = "ALLOCATED_HARD",
            actorType = AuditActorType.SYSTEM,
            actorId = "sys",
            role = "SYSTEM",
            timestamp = 1756880000000L,
            correlationId = "corr",
            sourceOperation = "OP"
        )
        val chain = SubstrateEnterpriseAuditEngine.computeChainHash(prevHash, recHash)

        // Tamper with record state
        val tamperedRecord = SubstrateEnterpriseAuditRecord(
            auditId = "TAMPERED-AUDIT-01",
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            reservationVersion = 1L,
            jobId = "JOB-01",
            orderId = "ORD-01",
            orderItemId = "ITEM-01",
            eventType = ReservationAuditEventType.HARD_ALLOCATED,
            previousState = null,
            newState = "TAMPERED_STATE",
            actorType = AuditActorType.SYSTEM,
            actorId = "sys",
            role = "SYSTEM",
            permissionContext = "TEST",
            timestamp = 1756880000000L,
            reason = "test reason",
            correlationId = "corr",
            sourceOperation = "OP",
            recordHash = recHash,
            previousAuditHash = prevHash,
            chainHash = chain
        )
        records.add(tamperedRecord)

        val result = SubstrateEnterpriseAuditEngine.verifyAuditChain(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            records = records,
            verifiedBy = "auditor"
        )

        assertFalse(result.isValidChain)
        assertEquals(IntegrityVerificationStatus.TAMPER_DETECTED, result.status)
        assertEquals(listOf("TAMPERED-AUDIT-01"), result.tamperedRecordIds)
    }

    @Test
    fun testReconciliation_healthyScenario() {
        val recon = SubstrateEnterpriseAuditEngine.reconcileReservation(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            orderId = "ORD-01",
            jobId = "JOB-01",
            sku = "ART-300",
            requiredSheets = 5000L,
            reservedSheets = 5000L,
            physicalOnHandSheets = 10000L,
            allocatedBatchSheets = 5000L,
            releasableSheets = 0L,
            consumedSheets = 0L,
            committedSheets = 5000L,
            replenishmentRequiredSheets = 0L,
            isProductionInProgress = false,
            reservationStatus = SubstrateReservationStatus.ALLOCATED_HARD,
            reconciledBy = "auditor"
        )

        assertEquals(ReconciliationStatus.HEALTHY, recon.status)
        assertTrue(recon.discrepancies.isEmpty())
    }

    @Test
    fun testReconciliation_detectsQuantityMismatchAndInventoryDeficit() {
        val recon = SubstrateEnterpriseAuditEngine.reconcileReservation(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            orderId = "ORD-01",
            jobId = "JOB-01",
            sku = "ART-300",
            requiredSheets = 5000L,
            reservedSheets = 3000L, // deficit
            physicalOnHandSheets = 2000L, // physical inventory less than reserved hold
            allocatedBatchSheets = 3000L,
            releasableSheets = 0L,
            consumedSheets = 0L,
            committedSheets = 0L,
            replenishmentRequiredSheets = 0L,
            isProductionInProgress = false,
            reservationStatus = SubstrateReservationStatus.ALLOCATED_HARD,
            reconciledBy = "auditor"
        )

        assertEquals(ReconciliationStatus.DISCREPANCIES_DETECTED, recon.status)
        assertEquals(3, recon.discrepancies.size)
        assertTrue(recon.discrepancies.any { it.discrepancyType == ReconciliationDiscrepancyType.QUANTITY_MISMATCH })
        assertTrue(recon.discrepancies.any { it.discrepancyType == ReconciliationDiscrepancyType.MISSING_INVENTORY_REFERENCE })
    }

    @Test
    fun testReconciliation_detectsConsumedButReserved() {
        val recon = SubstrateEnterpriseAuditEngine.reconcileReservation(
            tenantId = "TENANT-001",
            reservationId = "RES-01",
            orderId = "ORD-01",
            jobId = "JOB-01",
            sku = "ART-300",
            requiredSheets = 5000L,
            reservedSheets = 5000L,
            physicalOnHandSheets = 10000L,
            allocatedBatchSheets = 5000L,
            releasableSheets = 0L,
            consumedSheets = 5000L, // fully consumed on floor
            committedSheets = 0L,
            replenishmentRequiredSheets = 0L,
            isProductionInProgress = false,
            reservationStatus = SubstrateReservationStatus.ALLOCATED_HARD, // still marked hard hold
            reconciledBy = "auditor"
        )

        assertEquals(ReconciliationStatus.WARNING_DETECTED, recon.status)
        assertTrue(recon.discrepancies.any { it.discrepancyType == ReconciliationDiscrepancyType.CONSUMED_BUT_RESERVED })
    }

    @Test
    fun testSynthesizeEnterpriseHandoffContract_v6Synthesis() {
        val mockReservation = SubstrateReservation(
            reservationId = "RES-01",
            tenantId = "TENANT-001",
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

        val handoff = SubstrateEnterpriseAuditEngine.synthesizeEnterpriseHandoffContract(
            tenantId = "TENANT-001",
            reservation = mockReservation,
            batchSummary = "LOT-01 (5,000 sheets)",
            grainCompatibility = "GRAIN_COMPATIBLE",
            replenishmentState = "NORMAL",
            supplierAlertSent = false,
            releaseDecision = "NO_RELEASE_REQUIRED",
            releasableSheets = 0L,
            consumedSheets = 0L,
            productionCommitmentState = "COMMITTED",
            reconciliation = null,
            integrityResult = null,
            auditTrailCount = 5,
            latestAuditHash = "abc123hash"
        )

        assertEquals("6.0.0", handoff.contractVersion)
        assertTrue(handoff.isReadOnly)
        assertTrue(handoff.forbiddenActions.contains("MUTATE_RESERVATION_STATE"))
        assertTrue(handoff.forbiddenActions.contains("EXECUTE_SUBSTRATE_RELEASE"))
        assertEquals(64, handoff.masterIntegrityHash.length)
    }
}
