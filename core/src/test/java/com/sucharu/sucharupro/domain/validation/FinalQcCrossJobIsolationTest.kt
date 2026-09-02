package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-job reference isolation tests for Final QC (Module 06 Step 07).
 */
class FinalQcCrossJobIsolationTest {

    @Test
    fun defectCrossJob_rejected() {
        val defect = ProductionDefect(
            defectId = "def-01",
            productionJobId = "job-B",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Misalignment",
            description = "Color density offset",
            affectedQuantity = 20,
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validateDefectCrossJobIsolation("job-A", defect)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun reworkCrossJob_rejected() {
        val rework = ProductionRework(
            reworkId = "rew-01",
            projectId = "proj-01",
            productionJobId = "job-B",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 20,
            description = "Rework",
            requestedBy = "user-01",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validateReworkCrossJobIsolation("job-A", rework)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun reQcCrossJob_rejected() {
        val reQc = ReQcInspection(
            reQcId = "reqc-01",
            projectId = "proj-01",
            productionJobId = "job-B",
            productionReworkId = "rew-01",
            cycleNumber = 1,
            createdBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validateReQcCrossJobIsolation("job-A", reQc)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun checklistCrossJob_rejected() {
        val checklist = QcInspectionChecklist(
            inspectionChecklistId = "chk-01",
            inspectionId = "insp-01",
            checklistTemplateId = "tmpl-01",
            checklistTemplateVersion = 1,
            productionJobId = "job-B",
            productionQcId = "qc-01",
            createdAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validateChecklistCrossJobIsolation("job-A", checklist)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun preProductionQcCrossJob_rejected() {
        val qc = ProductionQc(
            qcId = "qc-01",
            productionJobId = "job-B",
            qcType = QcType.PRE_PRODUCTION,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validatePreProductionQcCrossJobIsolation("job-A", qc)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}
