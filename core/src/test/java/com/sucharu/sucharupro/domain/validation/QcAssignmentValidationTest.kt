package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validation tests for inspector assignment constraints (Module 06 Step 01).
 */
class QcAssignmentValidationTest {

    private val sampleQc = ProductionQc(
        qcId = "qc-asgn-val-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.DRAFT,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun assignment_blankInspectorName_fails() {
        val res = QcAssignmentValidator.validateAssignment(
            qc = sampleQc,
            inspectorId = "insp-01",
            inspectorName = "",
            callerRole = UserRole.MANAGER
        )
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("cannot be blank"))
    }

    @Test
    fun unassignment_whenNotAssigned_fails() {
        val res = QcAssignmentValidator.validateUnassignment(
            qc = sampleQc,
            callerRole = UserRole.MANAGER
        )
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("does not have an active inspector"))
    }
}
