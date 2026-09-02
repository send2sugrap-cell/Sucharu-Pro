package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-job and cross-inspection isolation tests for checklist responses (Module 06 Step 03).
 */
class QcChecklistCrossJobIsolationTest {

    private val itemA = QcChecklistItem(
        itemId = "item-A",
        checklistTemplateId = "tmpl-A",
        categoryId = "cat-A",
        title = "Job A Print Item",
        createdAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun response_withMismatchedChecklistItemId_failsValidation() {
        val crossResponse = QcInspectionResponse(
            responseId = "resp-B",
            inspectionId = "insp-B",
            checklistItemId = "item-UNRELATED", // Mismatched checklist item
            status = QcResponseStatus.PASS,
            respondedBy = "insp-01",
            respondedAt = "2026-08-16T10:00:00Z"
        )

        val res = QcInspectionResponseValidator.validateResponse(crossResponse, itemA)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("does not match Item"))
    }
}
