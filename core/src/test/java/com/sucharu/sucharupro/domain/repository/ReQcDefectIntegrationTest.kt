package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for defect integration and cross-job defect protection with Step 04 (Module 06 Step 06).
 */
class ReQcDefectIntegrationTest {

    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionReQcRepository

    @Before
    fun setUp() {
        runBlocking {
            reQcDataSource = FakeProductionReQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()
            defectDataSource = FakeProductionDefectDataSource()

            repository = ProductionReQcRepositoryImpl(
                reQcDataSource = reQcDataSource,
                reworkDataSource = reworkDataSource,
                defectDataSource = defectDataSource
            )

            reworkDataSource.insertRework(
                ProductionRework(
                    reworkId = "rew-001",
                    projectId = "proj-001",
                    productionJobId = "job-001",
                    reworkType = ReworkType.COLOR_CORRECTION,
                    reason = ReworkReason.DEFECT_CORRECTION,
                    status = ReworkStatus.RETURNED_TO_QC,
                    affectedQuantity = 100,
                    description = "Color fixed",
                    requestedBy = "user-01",
                    requestedAt = "2026-08-17T10:00:00Z",
                    createdAt = "2026-08-17T10:00:00Z",
                    updatedAt = "2026-08-17T10:00:00Z"
                )
            )

            defectDataSource.insertDefect(
                ProductionDefect(
                    defectId = "def-001",
                    productionJobId = "job-001",
                    category = DefectCategory.PRINT_QUALITY,
                    severity = DefectSeverity.MAJOR,
                    source = DefectSource.PRODUCTION_STAGE,
                    title = "Color mismatch",
                    description = "Delta-E > 3.0",
                    affectedQuantity = 100,
                    detectedAt = "2026-08-17T09:00:00Z",
                    detectedBy = "insp-01",
                    createdAt = "2026-08-17T09:00:00Z",
                    updatedAt = "2026-08-17T09:00:00Z"
                )
            )
        }
    }

    @Test
    fun createReQc_withValidDefect_success() = runBlocking {
        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            originalDefectId = "def-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val reQc = (result as DomainResult.Success).data
        assertEquals("def-001", reQc.originalDefectId)
    }

    @Test
    fun createReQc_withCrossJobDefect_fails() = runBlocking {
        defectDataSource.insertDefect(
            ProductionDefect(
                defectId = "def-job-B",
                productionJobId = "job-B",
                category = DefectCategory.PRINT_QUALITY,
                severity = DefectSeverity.MAJOR,
                source = DefectSource.PRODUCTION_STAGE,
                title = "Other job defect",
                description = "Other job",
                affectedQuantity = 50,
                detectedAt = "2026-08-17T09:00:00Z",
                detectedBy = "insp-01",
                createdAt = "2026-08-17T09:00:00Z",
                updatedAt = "2026-08-17T09:00:00Z"
            )
        )

        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            originalDefectId = "def-job-B",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}
