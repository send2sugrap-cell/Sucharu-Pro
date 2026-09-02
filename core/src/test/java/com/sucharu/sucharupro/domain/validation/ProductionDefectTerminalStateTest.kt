package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Terminal-state protection and immutability tests for [ProductionDefect] (Module 06 Step 04).
 */
class ProductionDefectTerminalStateTest {

    private val closedDefect = ProductionDefect(
        defectId = "def-term-01",
        productionJobId = "job-01",
        category = DefectCategory.PACKAGING_ERROR,
        severity = DefectSeverity.MINOR,
        source = DefectSource.SUPERVISOR_REPORTED,
        status = DefectStatus.CLOSED,
        title = "Damaged outer cartons",
        description = "5 cartons crushed during palletizing.",
        affectedQuantity = 5,
        resolutionNotes = "Repacked in new reinforced boxes",
        resolvedBy = "insp-01",
        resolvedAt = "2026-08-17T10:00:00Z",
        closedBy = "admin-01",
        closedAt = "2026-08-17T10:30:00Z",
        detectedAt = "2026-08-17T09:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T09:00:00Z",
        updatedAt = "2026-08-17T10:30:00Z"
    )

    private val cancelledDefect = closedDefect.copy(
        defectId = "def-term-02",
        status = DefectStatus.CANCELLED,
        closedBy = null,
        closedAt = null
    )

    @Test
    fun closedDefect_cannotTransitionToAnyStatus() {
        for (target in DefectStatus.entries) {
            val res = ProductionDefectLifecycleValidator.validateStatusTransition(closedDefect, target)
            assertFalse(res.isSuccess)
        }
    }

    @Test
    fun cancelledDefect_cannotTransitionToAnyStatus() {
        for (target in DefectStatus.entries) {
            val res = ProductionDefectLifecycleValidator.validateStatusTransition(cancelledDefect, target)
            assertFalse(res.isSuccess)
        }
    }

    @Test
    fun terminalDefect_cannotBeAssigned() {
        val res1 = ProductionDefectAssignmentValidator.validateAssignment(closedDefect, "tech-01", "Karim", UserRole.ADMIN)
        assertTrue(res1 is DomainResult.Error)

        val res2 = ProductionDefectAssignmentValidator.validateAssignment(cancelledDefect, "tech-01", "Karim", UserRole.ADMIN)
        assertTrue(res2 is DomainResult.Error)
    }

    @Test
    fun terminalDefect_cannotBeResolvedAgain() {
        val res = ProductionDefectValidator.validateResolution(closedDefect, "Again", "insp-01", UserRole.QC_INSPECTOR)
        assertTrue(res is DomainResult.Error)
    }
}
