package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-project and cross-job entity isolation validation tests (Module 06 Step 04).
 */
class ProductionDefectCrossProjectIsolationTest {

    @Test
    fun validator_detectsMismatchedJobsInQc() {
        val qc = ProductionQc(
            qcId = "qc-proj-101",
            productionJobId = "job-proj-101",
            qcType = QcType.PRE_PRODUCTION,
            status = QcStatus.FAILED,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val check1 = ProductionDefectValidator.validateQcCrossJobIsolation("job-proj-101", qc)
        assertTrue(check1 is DomainResult.Success)

        val check2 = ProductionDefectValidator.validateQcCrossJobIsolation("job-proj-999", qc)
        assertTrue(check2 is DomainResult.Error)
    }

    @Test
    fun validator_detectsMismatchedJobsInChecklist() {
        val checklist = QcInspectionChecklist(
            inspectionChecklistId = "chk-proj-101",
            inspectionId = "insp-101",
            checklistTemplateId = "tmpl-01",
            checklistTemplateVersion = 1,
            productionJobId = "job-proj-101",
            productionQcId = "qc-proj-101",
            status = QcChecklistStatus.READY,
            createdAt = "2026-08-17T10:00:00Z"
        )

        val check1 = ProductionDefectValidator.validateChecklistCrossJobIsolation("job-proj-101", checklist)
        assertTrue(check1 is DomainResult.Success)

        val check2 = ProductionDefectValidator.validateChecklistCrossJobIsolation("job-proj-999", checklist)
        assertTrue(check2 is DomainResult.Error)
    }
}
