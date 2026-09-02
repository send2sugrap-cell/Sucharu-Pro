package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.data.datasource.substratereservation.FakeSubstrateReplenishmentDataSource
import com.sucharu.sucharupro.data.repository.substratereservation.SubstrateReplenishmentRepositoryImpl
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class SubstrateReplenishmentServiceTest {

    private lateinit var service: SubstrateReplenishmentService
    private lateinit var dataSource: FakeSubstrateReplenishmentDataSource
    private val tenantId = "TENANT-SERVICE-TEST"

    private val policy = SubstrateReplenishmentPolicy(
        policyId = "POL-001",
        tenantId = tenantId,
        sku = "ART-300-25X36",
        minimumStockSheets = 2000L,
        safetyStockSheets = 5000L,
        reorderPointSheets = 12000L,
        targetStockSheets = 30000L,
        minimumOrderQuantitySheets = 5000L,
        standardPackReamSize = 500
    )

    private val vendors = listOf(
        Vendor(
            vendorId = "VND-001",
            projectId = tenantId,
            vendorCode = "VND-CENTURY",
            vendorName = "Century Paper Mills",
            status = VendorStatus.ACTIVE,
            primaryEmail = "sales@century.com"
        )
    )

    private val input = SubstrateReplenishmentEngine.EvaluationInput(
        tenantId = tenantId,
        productId = "P-101",
        sku = "ART-300-25X36",
        materialName = "Art Card 300",
        stockType = PaperStockType.ART_CARD,
        gsm = BigDecimal("300.0000"),
        sheetDimension = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS),
        warehouseId = "WH-1",
        warehouseName = "Central Depot",
        onHandPhysicalSheets = 8000L,
        activeReservedSheets = 3000L, // Available = 5000 <= 12000
        policy = policy,
        candidateVendors = vendors,
        evaluator = "test_planner"
    )

    @Before
    fun setup() {
        dataSource = FakeSubstrateReplenishmentDataSource()
        val repository = SubstrateReplenishmentRepositoryImpl(dataSource)
        service = SubstrateReplenishmentServiceImpl(repository)
    }

    @Test
    fun `test evaluateReplenishment creates record and records audit event`() = runBlocking {
        val eval = service.evaluateReplenishment(tenantId, input)

        assertNotNull(eval.evaluationId)
        assertEquals(ReplenishmentTriggerState.REORDER_TRIGGERED, eval.triggerState)
        assertTrue(eval.isReorderRequired)
        assertEquals(25000L, eval.recommendedReorderSheets)

        // Check audit event recorded
        val audits = dataSource.listAuditEvents(tenantId, eval.evaluationId)
        assertEquals(1, audits.size)
        assertEquals("EVALUATE_REPLENISHMENT", audits[0].triggerAction)
    }

    @Test
    fun `test repeated evaluateReplenishment with identical condition is idempotent`() = runBlocking {
        val eval1 = service.evaluateReplenishment(tenantId, input)
        val eval2 = service.evaluateReplenishment(tenantId, input)

        // Must return the exact same evaluation without creating a new ID or duplicate in repository
        assertEquals(eval1.evaluationId, eval2.evaluationId)
        assertEquals(eval1.deduplicationFingerprint, eval2.deduplicationFingerprint)
        val allEvals = service.listEvaluations(tenantId)
        assertEquals(1, allEvals.size)
    }

    @Test
    fun `test triggerSupplierAlert dispatches alert and updates evaluation state`() = runBlocking {
        val eval = service.evaluateReplenishment(tenantId, input)

        val alert = service.triggerSupplierAlert(tenantId, eval.evaluationId, vendorId = null, actor = "admin_user")

        assertNotNull(alert.alertId)
        assertEquals(eval.evaluationId, alert.evaluationId)
        assertEquals("VND-001", alert.vendorId)
        assertEquals("Century Paper Mills", alert.vendorName)
        assertEquals(25000L, alert.requestedSheets)

        // Verify evaluation state transitioned
        val updatedEval = service.getEvaluationById(tenantId, eval.evaluationId)
        assertNotNull(updatedEval)
        assertEquals(ReplenishmentTriggerState.SUPPLIER_ALERT_SENT, updatedEval!!.triggerState)

        // Verify audit log has 2 events (evaluate, alert)
        val audits = dataSource.listAuditEvents(tenantId, eval.evaluationId)
        assertEquals(2, audits.size)
        assertEquals("DISPATCH_SUPPLIER_ALERT", audits[1].triggerAction)
    }

    @Test
    fun `test updateReplenishmentStatus transitions state and records audit`() = runBlocking {
        val eval = service.evaluateReplenishment(tenantId, input)

        val updated = service.updateReplenishmentStatus(
            tenantId = tenantId,
            evaluationId = eval.evaluationId,
            newState = ReplenishmentTriggerState.PROCUREMENT_PENDING,
            reason = "PO #1049 issued by procurement team",
            actor = "procurement_lead"
        )

        assertEquals(ReplenishmentTriggerState.PROCUREMENT_PENDING, updated.triggerState)
        val audits = dataSource.listAuditEvents(tenantId, eval.evaluationId)
        assertEquals(2, audits.size)
        assertEquals("STATUS_UPDATE", audits[1].triggerAction)
    }

    @Test
    fun `test exportHandoffContract produces version 4_0_0 contract`() = runBlocking {
        val eval = service.evaluateReplenishment(tenantId, input)
        val contract = service.exportHandoffContract(tenantId, eval.evaluationId)

        assertEquals("4.0.0", contract.contractVersion)
        assertEquals(eval.evaluationId, contract.evaluationId)
        assertEquals("ART-300-25X36", contract.sku)
        assertEquals(eval.deduplicationFingerprint, contract.deduplicationFingerprint)
        assertEquals(eval.masterIntegrityHash, contract.masterIntegrityHash)
    }
}
