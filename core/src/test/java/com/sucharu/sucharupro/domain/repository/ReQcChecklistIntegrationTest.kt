package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.ProductionReQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
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
 * Tests for checklist integration and cross-job protection with Step 03 (Module 06 Step 06).
 */
class ReQcChecklistIntegrationTest {

    private lateinit var reQcDataSource: FakeProductionReQcDataSource
    private lateinit var reworkDataSource: FakeProductionReworkDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var repository: ProductionReQcRepository

    @Before
    fun setUp() {
        runBlocking {
            reQcDataSource = FakeProductionReQcDataSource()
            reworkDataSource = FakeProductionReworkDataSource()
            checklistDataSource = FakeQcChecklistDataSource()

            repository = ProductionReQcRepositoryImpl(
                reQcDataSource = reQcDataSource,
                reworkDataSource = reworkDataSource,
                checklistDataSource = checklistDataSource
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

            checklistDataSource.insertInspectionChecklist(
                QcInspectionChecklist(
                    inspectionChecklistId = "chk-001",
                    inspectionId = "insp-001",
                    checklistTemplateId = "tmpl-001",
                    checklistTemplateVersion = 1,
                    productionJobId = "job-001",
                    productionQcId = "qc-001",
                    createdAt = "2026-08-17T10:00:00Z"
                )
            )
        }
    }

    @Test
    fun createReQc_withValidChecklist_success() = runBlocking {
        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            checklistId = "chk-001",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val reQc = (result as DomainResult.Success).data
        assertEquals("chk-001", reQc.checklistId)
    }

    @Test
    fun createReQc_withCrossJobChecklist_fails() = runBlocking {
        checklistDataSource.insertInspectionChecklist(
            QcInspectionChecklist(
                inspectionChecklistId = "chk-job-B",
                inspectionId = "insp-002",
                checklistTemplateId = "tmpl-001",
                checklistTemplateVersion = 1,
                productionJobId = "job-B",
                productionQcId = "qc-002",
                createdAt = "2026-08-17T10:00:00Z"
            )
        )

        val result = repository.createReQc(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            checklistId = "chk-job-B",
            createdBy = "insp-01",
            timestamp = "2026-08-17T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}
