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
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests enforcing cross-job isolation for Re-QC (Module 06 Step 06).
 */
class ReQcCrossJobIsolationTest {

    @Test
    fun reworkCrossJob_rejected() {
        val rework = ProductionRework(
            reworkId = "rew-001",
            projectId = "proj-001",
            productionJobId = "job-B",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = ReworkStatus.RETURNED_TO_QC,
            affectedQuantity = 50,
            description = "Fix color",
            requestedBy = "user-01",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ReQcValidator.validateSourceRework("job-A", rework)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun defectCrossJob_rejected() {
        val defect = ProductionDefect(
            defectId = "def-001",
            productionJobId = "job-B",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            title = "Misregistration",
            description = "Cyan misalignment",
            affectedQuantity = 25,
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ReQcValidator.validateDefectCrossJobIsolation("job-A", defect)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun qcCrossJob_rejected() {
        val qc = ProductionQc(
            qcId = "qc-001",
            productionJobId = "job-B",
            qcType = QcType.PRE_PRODUCTION,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ReQcValidator.validateQcCrossJobIsolation("job-A", qc)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun checklistCrossJob_rejected() {
        val checklist = QcInspectionChecklist(
            inspectionChecklistId = "chk-001",
            inspectionId = "insp-001",
            checklistTemplateId = "tmpl-001",
            checklistTemplateVersion = 1,
            productionJobId = "job-B",
            productionQcId = "qc-001",
            createdAt = "2026-08-17T10:00:00Z"
        )

        val result = ReQcValidator.validateChecklistCrossJobIsolation("job-A", checklist)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}
