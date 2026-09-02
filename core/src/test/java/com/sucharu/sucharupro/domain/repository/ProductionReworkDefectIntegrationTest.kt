package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying integration between [ProductionDefect] and [ProductionRework] (Module 06 Step 05).
 */
class ProductionReworkDefectIntegrationTest {

    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() = runBlocking {
        reworkDataSource = FakeProductionReworkDataSource()
        defectDataSource = FakeProductionDefectDataSource()

        // Seed an existing defect
        val defect = ProductionDefect(
            defectId = "def-101",
            productionJobId = "job-01",
            category = DefectCategory.LAMINATION_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.CHECKLIST_INSPECTION,
            status = DefectStatus.OPEN,
            title = "Lamination Bubbles",
            description = "Bubbles formed on matte thermal lamination",
            affectedQuantity = 150,
            affectedUnit = "sheets",
            detectedAt = "2026-08-17T09:30:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T09:30:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )
        defectDataSource.insertDefect(defect)

        repository = ProductionReworkRepositoryImpl(
            reworkDataSource = reworkDataSource,
            defectDataSource = defectDataSource
        )
    }

    @Test
    fun createRework_fromMatchingDefect_succeedsAndPreservesLinkage() = runBlocking {
        val result = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            defectId = "def-101",
            reworkType = ReworkType.LAMINATION_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 150,
            quantityUnit = "sheets",
            description = "Re-laminate affected 150 sheets after temperature adjustment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val rework = (result as DomainResult.Success).data
        assertEquals("def-101", rework.defectId)
        assertEquals("job-01", rework.productionJobId)
        assertEquals("proj-01", rework.projectId)
    }

    @Test
    fun createRework_withDefectFromDifferentJob_failsCrossJobIsolation() = runBlocking {
        val result = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-999", // Mismatched job
            defectId = "def-101",
            reworkType = ReworkType.LAMINATION_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 150,
            quantityUnit = "sheets",
            description = "Re-laminate",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Cross-job reference violation"))
    }
}
