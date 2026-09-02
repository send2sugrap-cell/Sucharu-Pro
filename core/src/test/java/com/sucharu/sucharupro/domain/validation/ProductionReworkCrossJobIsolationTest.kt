package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-Job Isolation unit tests ensuring rework cannot reference defects, QC, or checklists from different jobs (Module 06 Step 05).
 */
class ProductionReworkCrossJobIsolationTest {

    @Test
    fun validateDefectCrossJobIsolation_matchingJobId_succeeds() {
        val defect = ProductionDefect(
            defectId = "def-01",
            productionJobId = "job-100",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.CHECKLIST_INSPECTION,
            status = DefectStatus.OPEN,
            title = "Print Streaks",
            description = "Cyan roller streak",
            affectedQuantity = 50,
            affectedUnit = "pcs",
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ProductionReworkValidator.validateDefectCrossJobIsolation("job-100", defect)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateDefectCrossJobIsolation_differentJobId_fails() {
        val defect = ProductionDefect(
            defectId = "def-01",
            productionJobId = "job-999",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.CHECKLIST_INSPECTION,
            status = DefectStatus.OPEN,
            title = "Print Streaks",
            description = "Cyan roller streak",
            affectedQuantity = 50,
            affectedUnit = "pcs",
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ProductionReworkValidator.validateDefectCrossJobIsolation("job-100", defect)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun validateQcCrossJobIsolation_differentJobId_fails() {
        val qc = ProductionQc(
            qcId = "qc-01",
            productionJobId = "job-999",
            qcType = QcType.FINAL,
            status = QcStatus.FAILED,
            decision = QcDecision.FAIL,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = ProductionReworkValidator.validateQcCrossJobIsolation("job-100", qc)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }

    @Test
    fun validateChecklistCrossJobIsolation_differentJobId_fails() {
        val checklist = QcInspectionChecklist(
            inspectionChecklistId = "chk-01",
            inspectionId = "insp-01",
            checklistTemplateId = "tpl-01",
            checklistTemplateVersion = 1,
            productionJobId = "job-999",
            productionQcId = "qc-01",
            status = QcChecklistStatus.COMPLETED,
            createdAt = "2026-08-17T10:00:00Z"
        )

        val result = ProductionReworkValidator.validateChecklistCrossJobIsolation("job-100", checklist)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-job reference violation"))
    }
}
