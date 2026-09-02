package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for terminal-state immutability on completed inspection checklists (Module 06 Step 03).
 */
class QcChecklistImmutabilityTest {

    private val completedChecklist = QcInspectionChecklist(
        inspectionChecklistId = "chk-comp-01",
        inspectionId = "insp-01",
        checklistTemplateId = "tmpl-01",
        checklistTemplateVersion = 1,
        productionJobId = "job-01",
        productionQcId = "qc-01",
        status = QcChecklistStatus.COMPLETED,
        createdAt = "2026-08-16T10:00:00Z",
        completedAt = "2026-08-16T11:00:00Z"
    )

    @Test
    fun completedChecklist_cannotTransitionToOtherStates() {
        val res = QcInspectionChecklistValidator.validateStatusTransition(
            checklist = completedChecklist,
            targetStatus = QcChecklistStatus.IN_PROGRESS
        )
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Cannot modify terminal inspection checklist"))
    }
}
