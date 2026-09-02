package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.datasource.imposition.FakeImpositionDataSource
import com.sucharu.sucharupro.data.repository.imposition.ImpositionRepositoryImpl
import com.sucharu.sucharupro.domain.model.imposition.ImpositionOrientationPolicy
import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ImpositionServiceTest {

    private lateinit var dataSource: FakeImpositionDataSource
    private lateinit var repository: ImpositionRepositoryImpl
    private lateinit var service: ImpositionServiceImpl

    @Before
    fun setup() {
        dataSource = FakeImpositionDataSource()
        repository = ImpositionRepositoryImpl(dataSource)
        service = ImpositionServiceImpl(repository)
    }

    @Test
    fun testCalculateAndSave_PersistsAndRetrievesSpecification() = runBlocking {
        val itemDim = PrintingDimension(BigDecimal("210.0000"), BigDecimal("297.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("635.0000"), BigDecimal("914.4000"), MeasurementUnit.MILLIMETERS)

        val spec = service.calculateAndSave(
            tenantId = "TENANT-SERVICE-01",
            jobId = "JOB-SRV-01",
            orderId = "ORD-SRV-01",
            orderItemId = "ITEM-SRV-01",
            productName = "Service Test Item",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 5000L
        )

        assertNotNull(spec)
        assertTrue(spec.copiesPerSheet > 0)

        val fetched = service.getImpositionSpecification("TENANT-SERVICE-01", spec.impositionId)
        assertNotNull(fetched)
        assertEquals(spec.impositionId, fetched?.impositionId)
        assertEquals("TENANT-SERVICE-01", fetched?.tenantId)

        val byJob = service.listImpositionsByJob("TENANT-SERVICE-01", "JOB-SRV-01")
        assertEquals(1, byJob.size)
        assertEquals(spec.impositionId, byJob.first().impositionId)

        val all = service.listAllImpositions("TENANT-SERVICE-01")
        assertEquals(1, all.size)
    }

    @Test
    fun testExportHandoffContract_GeneratesModule19CompatibleHandoff() = runBlocking {
        val itemDim = PrintingDimension(BigDecimal("100.0000"), BigDecimal("150.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("500.0000"), BigDecimal("700.0000"), MeasurementUnit.MILLIMETERS)

        val spec = service.calculateAndSave(
            tenantId = "TENANT-HANDOFF-01",
            jobId = "JOB-HO-01",
            orderId = "ORD-HO-01",
            orderItemId = "ITEM-HO-01",
            productName = "Handoff Contract Item",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 2000L
        )

        val handoff = service.exportHandoffContract("TENANT-HANDOFF-01", spec.impositionId)

        assertEquals("1.0.0", handoff.contractVersion)
        assertEquals(spec.impositionId, handoff.impositionId)
        assertEquals("TENANT-HANDOFF-01", handoff.tenantId)
        assertEquals(spec.parentSheetDimension.width, handoff.parentSheetWidthMm)
        assertEquals(spec.parentSheetDimension.height, handoff.parentSheetHeightMm)
        assertEquals(spec.copiesPerSheet, handoff.copiesPerSheet)
        assertEquals(spec.requiredSheets, handoff.requiredProductiveSheets)
        assertEquals(spec.yieldPercentage, handoff.sheetYieldPercentage)
    }

    @Test
    fun testUpdateImpositionStatus_UpdatesLifecycleCorrectly() = runBlocking {
        val itemDim = PrintingDimension(BigDecimal("100.0000"), BigDecimal("100.0000"), MeasurementUnit.MILLIMETERS)
        val sheetDim = PrintingDimension(BigDecimal("500.0000"), BigDecimal("500.0000"), MeasurementUnit.MILLIMETERS)

        val spec = service.calculateAndSave(
            tenantId = "TENANT-STATUS-01",
            jobId = "JOB-STAT-01",
            orderId = "ORD-STAT-01",
            orderItemId = "ITEM-STAT-01",
            productName = "Status Test Item",
            finishedItemDimension = itemDim,
            parentSheetDimension = sheetDim,
            requiredQuantity = 1000L
        )

        val ok = service.updateImpositionStatus(
            tenantId = "TENANT-STATUS-01",
            impositionId = spec.impositionId,
            status = "APPLIED_TO_PLANNING",
            actor = "planner_user",
            notes = "Assigned to Offset Press 01"
        )

        assertTrue(ok)
        val updated = service.getImpositionSpecification("TENANT-STATUS-01", spec.impositionId)
        assertEquals("APPLIED_TO_PLANNING", updated?.status?.name)
        assertEquals("Assigned to Offset Press 01", updated?.notes)
    }
}
