package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying activity type extensions for Approval Workflow (Module 05 Step 04).
 */
class ApprovalAuditTest {

    @Test
    fun designActivityTypes_includeStep04ApprovalActivities() {
        val expectedTypes = listOf(
            DesignActivityType.APPROVAL_REQUESTED,
            DesignActivityType.APPROVAL_REVIEW_STARTED,
            DesignActivityType.APPROVAL_APPROVED,
            DesignActivityType.APPROVAL_REVISION_REQUIRED,
            DesignActivityType.APPROVAL_REJECTED,
            DesignActivityType.APPROVAL_RESUBMITTED,
            DesignActivityType.APPROVAL_FINAL_LOCKED
        )

        expectedTypes.forEach { type ->
            assertTrue(type.name.isNotBlank())
            assertTrue(type.defaultLabel.isNotBlank())
        }
    }
}
