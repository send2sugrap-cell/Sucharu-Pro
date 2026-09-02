package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.qc.QcActivityType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for QC Checklist activity event types (Module 06 Step 03).
 */
class QcChecklistAuditTest {

    @Test
    fun checklistActivityTypes_definedCorrectly() {
        val expected = listOf(
            QcActivityType.QC_CHECKLIST_TEMPLATE_CREATED,
            QcActivityType.QC_CHECKLIST_TEMPLATE_VERSION_CREATED,
            QcActivityType.QC_CHECKLIST_ACTIVATED,
            QcActivityType.QC_CHECKLIST_DEACTIVATED,
            QcActivityType.QC_INSPECTION_CHECKLIST_CREATED,
            QcActivityType.QC_INSPECTION_CHECKLIST_STARTED,
            QcActivityType.QC_INSPECTION_RESPONSE_UPDATED,
            QcActivityType.QC_INSPECTION_CHECKLIST_COMPLETED,
            QcActivityType.QC_INSPECTION_PASSED,
            QcActivityType.QC_INSPECTION_FAILED
        )

        expected.forEach { type ->
            assertTrue(type.name.isNotBlank())
            assertTrue(type.defaultLabel.isNotBlank())
        }
    }
}
