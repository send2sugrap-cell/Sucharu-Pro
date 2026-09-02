package com.sucharu.sucharupro.ui.features.production.finalqc

import com.sucharu.sucharupro.data.datasource.finalqc.FakeFinalQcPackagingDataSource
import com.sucharu.sucharupro.data.repository.finalqc.FinalQcPackagingRepositoryImpl
import com.sucharu.sucharupro.domain.model.finalqc.*
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.service.finalqc.FinalQcPackagingServiceImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FinalQcPackagingViewModelTest {

    private lateinit var viewModel: FinalQcPackagingViewModel
    private lateinit var fakeDs: FakeFinalQcPackagingDataSource
    private val tenantId = "TENANT-001"
    private val jobId = "JOB-101"

    @Before
    fun setup() {
        fakeDs = FakeFinalQcPackagingDataSource()
        val repo = FinalQcPackagingRepositoryImpl(fakeDs)
        val service = FinalQcPackagingServiceImpl(repo)

        viewModel = FinalQcPackagingViewModel(
            finalQcService = service,
            defaultTenantId = tenantId,
            externalScope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `test creating inspection and fetching final QC data updates UI state`() = runBlocking {
        viewModel.createInspection(
            jobId = jobId,
            orderId = "ORD-101",
            samplePlanType = InspectionSamplePlanType.AQL_LEVEL_II_NORMAL,
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("200.0000"),
            checklist = listOf(QcChecklistItem("CHK-01", "Registration", true)),
            inspectorId = "INSP-01",
            inspectorName = "Tariq QC"
        )

        val state = viewModel.uiState.value
        assertEquals(1, state.inspections.size)
        assertEquals("IN_PROGRESS", state.inspections.first().status)
        assertEquals(BigDecimal("5000.0000"), state.inspections.first().totalLotQuantity)
        assertFalse(state.isInspectionDialogOpen)
    }

    @Test
    fun `test complete inspection, packaging and release updates state correctly`() = runBlocking {
        // 1. Create and complete inspection
        viewModel.createInspection(
            jobId = jobId,
            orderId = "ORD-101",
            samplePlanType = InspectionSamplePlanType.FULL_100_PERCENT,
            totalLotQuantity = BigDecimal("5000.0000"),
            sampleSize = BigDecimal("5000.0000"),
            checklist = emptyList(),
            inspectorId = "INSP-01",
            inspectorName = "Tariq QC"
        )

        val inspId = viewModel.uiState.value.inspections.first().inspectionId
        viewModel.completeInspection(
            inspectionId = inspId,
            acceptedQty = BigDecimal("4950.0000"),
            rejectedQty = BigDecimal("50.0000")
        )

        // 2. Defect containment
        viewModel.recordDefectContainment(
            jobId = jobId,
            inspectionId = inspId,
            rootCauseStage = ProductionStageType.PRINTING,
            defectType = DefectClassificationType.PRINTING_DEFECT,
            severity = DefectSeverity.MAJOR,
            defectQty = BigDecimal("50.0000"),
            disposition = ContainmentDisposition.QUARANTINED,
            quarantineLocation = "BAY-Q1",
            rootCauseDetails = "Hickey marks"
        )

        // 3. Packaging
        viewModel.createPackagingRecord(
            jobId = jobId,
            inspectionId = inspId,
            packagingType = PackagingType.CORRUGATED_BOX,
            unitsPerPackage = BigDecimal("495.0000"),
            totalPackageCount = 10,
            packagedBy = "Packer Lead"
        )

        val pkgId = viewModel.uiState.value.packagingRecords.first().packagingId

        // 4. Release
        viewModel.authorizeRelease(
            jobId = jobId,
            orderId = "ORD-101",
            inspectionId = inspId,
            packagingId = pkgId,
            releasedQty = BigDecimal("4950.0000"),
            destination = "WAREHOUSE_FINISHED_GOODS",
            authorizedBy = "Plant Manager"
        )

        val finalState = viewModel.uiState.value
        assertEquals(1, finalState.inspections.size)
        assertEquals(1, finalState.defects.size)
        assertEquals(1, finalState.packagingRecords.size)
        assertEquals(1, finalState.releaseRecords.size)
        assertEquals(BigDecimal("4950.0000"), finalState.releaseRecords.first().releasedQuantity)
        assertNotNull(finalState.varianceSummary)
        assertNotNull(finalState.reconciliationResult)
        assertTrue(finalState.reconciliationResult?.isFullyReconciled == true)
    }
}
