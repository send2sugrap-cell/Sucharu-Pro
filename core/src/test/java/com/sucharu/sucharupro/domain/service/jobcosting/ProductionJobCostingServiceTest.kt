package com.sucharu.sucharupro.domain.service.jobcosting

import com.sucharu.sucharupro.data.datasource.jobcosting.FakeProductionJobCostingDataSource
import com.sucharu.sucharupro.data.repository.jobcosting.ProductionJobCostingRepositoryImpl
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.jobcosting.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.shopfloortracking.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionJobCostingServiceTest {

    private lateinit var service: ProductionJobCostingService
    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-101"
    private val orderId = "ORD-101"

    @Before
    fun setup() {
        val fakeDs = FakeProductionJobCostingDataSource()
        val repo = ProductionJobCostingRepositoryImpl(fakeDs)
        service = ProductionJobCostingServiceImpl(repo)
    }

    @Test
    fun `test complete job costing, variance calculation, and reconciliation workflow`() = runBlocking {
        val materials = listOf(
            ProductionMaterialConsumptionRecord(
                consumptionId = "MAT-01",
                tenantId = tenantId,
                workOrderId = "WO-01",
                executionJobId = executionJobId,
                stageType = ProductionStageType.PRINTING,
                materialCode = "SUB-01",
                materialName = "Paper",
                unitOfMeasure = "SHEETS",
                plannedQuantity = BigDecimal("1000.0000"),
                actualQuantityConsumed = BigDecimal("1000.0000"),
                scrapQuantity = BigDecimal.ZERO,
                varianceQuantity = BigDecimal.ZERO,
                variancePercentage = BigDecimal.ZERO,
                batchLotNumber = "LOT-01",
                recordedBy = "operator"
            )
        )


        // 1. Calculate actual job cost
        val actualCost = service.calculateActualJobCost(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            manufacturedGoodQuantity = BigDecimal("1000.0000"),
            materialConsumptions = materials,
            packagingUnitRate = BigDecimal("20.0000"),
            overheadAllocationRate = BigDecimal("0.1000")
        )
        assertNotNull(actualCost)
        assertEquals(JobCostStatus.ACTUAL_COSTED, actualCost.costStatus)

        // 2. Calculate variance
        val variance = service.calculateJobCostVariance(
            tenantId = tenantId,
            executionJobId = executionJobId,
            quotedSellingPrice = BigDecimal("15000.0000"),
            estimatedTotalCost = BigDecimal("10000.0000"),
            estimatedMaterialCost = BigDecimal("2500.0000"),
            estimatedLaborCost = BigDecimal("1000.0000"),
            estimatedMachineCost = BigDecimal("500.0000"),
            orderQuantity = BigDecimal("1000.0000")
        )
        assertNotNull(variance)

        // 3. Reconcile
        val recon = service.reconcileJobCosting(tenantId, executionJobId)
        assertTrue(recon.isFullyReconciled)
        assertTrue(recon.actualCostMathBalanced)

        // 4. Export AI Handoff
        val handoff = service.getAiHandoffContract(tenantId, executionJobId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertTrue(handoff.isFullyReconciled)
        assertEquals(recon.certificateHash, handoff.costCertificateHash)
    }
}
