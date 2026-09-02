package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for immutable Pre-Production QC Specification Snapshot recording (Module 06 Step 02).
 */
class PreProductionQcSpecificationSnapshotTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-snap-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.IN_INSPECTION,
        assignedInspectorId = "insp-01",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        runBlocking {
            qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
            qcRepository = ProductionQcRepositoryImpl(qcDataSource)
            qcRepository.initializePreProductionItems("qc-snap-01", UserRole.QC_INSPECTOR)

            // Mark all items as PASS
            val items = qcRepository.observePreProductionItems("qc-snap-01").first()
            items.forEach { item ->
                qcRepository.updatePreProductionItem(
                    itemId = item.itemId,
                    status = PreProductionItemStatus.PASS,
                    checkedBy = "insp-01",
                    timestamp = "2026-08-16T10:15:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }
    }

    @Test
    fun submitQc_withSnapshot_persistsImmutableSnapshot() = runBlocking {
        val snapshot = PreProductionQcSnapshot(
            snapshotId = "snap-01",
            qcId = "qc-snap-01",
            productionJobId = "job-01",
            jobTitle = "Premium Catalog",
            quantity = 1000,
            width = 8.5,
            height = 11.0,
            unit = "inch",
            colorSpecification = "4 Color CMYK",
            materialSpecification = "150 GSM Art Paper",
            finishingSpecification = "Matte Lamination + Spot UV",
            bleed = "3mm",
            trim = "Exact",
            safeArea = "5mm",
            resolution = "300 DPI",
            artworkId = "art-01",
            artworkVersionId = "av-1",
            proofId = "proof-01",
            proofVersionId = "pv-1",
            approvalId = "app-01",
            inspectedAt = "2026-08-16T11:00:00Z",
            inspectedBy = "insp-01",
            inspectedByName = "Inspector 1"
        )

        val submitRes = qcRepository.submitPreProductionQc(
            qcId = "qc-snap-01",
            decision = QcDecision.PASS,
            snapshot = snapshot,
            submittedBy = "insp-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(submitRes is DomainResult.Success)
        val recordedSnapshot = qcRepository.getPreProductionSnapshot("qc-snap-01").first()
        assertNotNull(recordedSnapshot)
        assertEquals("Premium Catalog", recordedSnapshot?.jobTitle)
        assertEquals(1000, recordedSnapshot?.quantity)
        assertEquals("150 GSM Art Paper", recordedSnapshot?.materialSpecification)
    }
}
