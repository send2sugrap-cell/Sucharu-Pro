package com.sucharu.sucharupro.domain.service.jobclosure

import com.sucharu.sucharupro.data.datasource.jobclosure.FakeProductionJobClosureDataSource
import com.sucharu.sucharupro.data.repository.jobclosure.ProductionJobClosureRepositoryImpl
import com.sucharu.sucharupro.domain.model.jobclosure.JobClosureStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionJobClosureServiceTest {

    private lateinit var service: ProductionJobClosureService
    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-101"
    private val orderId = "ORD-101"

    @Before
    fun setup() {
        val fakeDs = FakeProductionJobClosureDataSource()
        val repo = ProductionJobClosureRepositoryImpl(fakeDs)
        service = ProductionJobClosureServiceImpl(repo)
    }

    @Test
    fun `test complete job closure, scorecard evaluation, and AI handoff contract export`() = runBlocking {
        // 1. Close and seal job
        val closure = service.closeAndSealJob(
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            orderQuantity = BigDecimal("5000.0000"),
            goodUnitsReleased = BigDecimal("5000.0000"),
            estimatedTotalCost = BigDecimal("20000.0000"),
            actualTotalCost = BigDecimal("20000.0000"),
            totalCostVariance = BigDecimal.ZERO,
            reworkOrScrapUnits = BigDecimal.ZERO,
            machineEfficiency = BigDecimal("92.0000"),
            onTime = true,
            actor = "plant-manager"
        )

        assertNotNull(closure)
        assertEquals(JobClosureStatus.GOVERNANCE_SEALED, closure.closureStatus)
        assertEquals(64, closure.masterCertificate.masterSealHash.length)
        assertTrue(closure.provenanceGraph.isChainUnbroken)

        // 2. Fetch scorecard
        val scorecard = service.getScorecardByJob(tenantId, executionJobId)
        assertNotNull(scorecard)
        assertEquals(BigDecimal("100.0000"), scorecard?.onTimeInFullPercentage)

        // 3. Export AI handoff
        val handoff = service.getAiHandoffContract(tenantId, executionJobId)
        assertEquals("1.0.0", handoff.contractVersion)
        assertTrue(handoff.isReadyForClosure)
        assertTrue(handoff.crossModuleInventoryConfirmed)
        assertTrue(handoff.crossModuleDeliveryConfirmed)
        assertTrue(handoff.crossModuleFinanceConfirmed)
        assertTrue(handoff.crossModuleProfitabilityLocked)
    }
}
