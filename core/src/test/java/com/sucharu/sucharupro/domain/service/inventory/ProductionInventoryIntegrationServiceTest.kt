package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.data.datasource.FakeFinishedProductInventoryDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryProductDataSource
import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.finalqc.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ProductionInventoryIntegrationServiceTest {

    private lateinit var finishedProductInventoryDataSource: FakeFinishedProductInventoryDataSource
    private lateinit var finalQcPackagingDataSource: FakeFinalQcPackagingDataSource
    private lateinit var inventoryProductDataSource: FakeInventoryProductDataSource
    private lateinit var service: ProductionInventoryIntegrationService

    private val tenantId = "TENANT-001"
    private val executionJobId = "JOB-101"
    private val orderId = "ORD-501"
    private val warehouseId = "WH-MAIN"

    @Before
    fun setUp() {
        finishedProductInventoryDataSource = FakeFinishedProductInventoryDataSource()
        finalQcPackagingDataSource = FakeFinalQcPackagingDataSource()
        inventoryProductDataSource = FakeInventoryProductDataSource()
        service = ProductionInventoryIntegrationServiceImpl(
            finishedProductInventoryDataSource = finishedProductInventoryDataSource,
            finalQcPackagingDataSource = finalQcPackagingDataSource,
            inventoryProductDataSource = inventoryProductDataSource
        )
    }

    @Test
    fun testJobWithoutFinalQc_IsRefused() = runBlocking {
        val eligibilityRes = service.evaluateInventoryEligibility(tenantId, executionJobId)
        assertTrue(eligibilityRes is DomainResult.Success)
        val eligibility = (eligibilityRes as DomainResult.Success).data
        assertFalse(eligibility.isEligible)
        assertTrue(eligibility.refusalReasons.isNotEmpty())

        val receiveRes = service.receiveFinishedGoodsFromProduction(
            tenantId = tenantId,
            executionJobId = executionJobId,
            warehouseId = warehouseId,
            actor = "qc_manager"
        )
        assertTrue(receiveRes is DomainResult.Error)
    }

    @Test
    fun testJobWithPassedFinalQc_IsEligibleAndReceived() = runBlocking {
        val inspection = ProductionFinalQcInspection(
            inspectionId = "INSP-101",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            samplePlanType = InspectionSamplePlanType.FULL_100_PERCENT,
            totalLotQuantity = BigDecimal("1000"),
            sampleSize = BigDecimal("1000"),
            acceptedQuantity = BigDecimal("980"),
            rejectedQuantity = BigDecimal("20"),
            reworkQuantity = BigDecimal.ZERO,
            status = FinalQcInspectionStatus.ACCEPTED,
            inspectorId = "INSP-USR-01",
            inspectorName = "Chief Inspector"
        )
        finalQcPackagingDataSource.saveInspection(tenantId, inspection)

        val release = FinishedGoodsReleaseRecord(
            releaseId = "REL-101",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            inspectionId = "INSP-101",
            packagingId = "PKG-101",
            releasedQuantity = BigDecimal("980"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            status = FinishedGoodsReleaseStatus.RELEASE_APPROVED,
            authorizedBy = "RELEASE-AUTH-01",
            integrityHash = "hash-123"
        )
        finalQcPackagingDataSource.saveReleaseRecord(tenantId, release)

        val eligibilityRes = service.evaluateInventoryEligibility(tenantId, executionJobId)
        assertTrue(eligibilityRes is DomainResult.Success)
        val eligibility = (eligibilityRes as DomainResult.Success).data
        assertTrue(eligibility.isEligible)
        assertEquals(BigDecimal("980"), eligibility.eligibleQuantity)

        val receiveRes = service.receiveFinishedGoodsFromProduction(
            tenantId = tenantId,
            executionJobId = executionJobId,
            warehouseId = warehouseId,
            actor = "warehouse_mgr"
        )
        assertTrue(receiveRes is DomainResult.Success)
        val receipt = (receiveRes as DomainResult.Success).data
        assertEquals(executionJobId, receipt.executionJobId)
        assertEquals(orderId, receipt.orderId)
        assertEquals(BigDecimal("980"), receipt.receivedQuantity)

        // Test Idempotency: second receive call returns same receipt
        val secondReceiveRes = service.receiveFinishedGoodsFromProduction(
            tenantId = tenantId,
            executionJobId = executionJobId,
            warehouseId = warehouseId,
            actor = "warehouse_mgr"
        )
        assertTrue(secondReceiveRes is DomainResult.Success)
        val secondReceipt = (secondReceiveRes as DomainResult.Success).data
        assertEquals(receipt.receiptId, secondReceipt.receiptId)
    }

    @Test
    fun testJobWithFailedQc_IsRefused() = runBlocking {
        val inspection = ProductionFinalQcInspection(
            inspectionId = "INSP-102",
            tenantId = tenantId,
            executionJobId = executionJobId,
            orderId = orderId,
            samplePlanType = InspectionSamplePlanType.FULL_100_PERCENT,
            totalLotQuantity = BigDecimal("1000"),
            sampleSize = BigDecimal("1000"),
            acceptedQuantity = BigDecimal.ZERO,
            rejectedQuantity = BigDecimal("1000"),
            reworkQuantity = BigDecimal.ZERO,
            status = FinalQcInspectionStatus.REJECTED,
            inspectorId = "INSP-USR-01",
            inspectorName = "Chief Inspector"
        )
        finalQcPackagingDataSource.saveInspection(tenantId, inspection)

        val eligibilityRes = service.evaluateInventoryEligibility(tenantId, executionJobId)
        assertTrue(eligibilityRes is DomainResult.Success)
        val eligibility = (eligibilityRes as DomainResult.Success).data
        assertFalse(eligibility.isEligible)
    }
}
