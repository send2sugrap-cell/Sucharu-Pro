package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive transition matrix tests for [DefectStatus] state graph (Module 06 Step 04).
 */
class ProductionDefectStatusTransitionTest {

    private fun buildDefect(status: DefectStatus): ProductionDefect {
        return ProductionDefect(
            defectId = "def-stat-01",
            productionJobId = "job-01",
            category = DefectCategory.DIE_CUT_ERROR,
            severity = DefectSeverity.MINOR,
            source = DefectSource.OPERATOR_REPORTED,
            status = status,
            title = "Creasing crease off by 1mm",
            description = "Crease location slightly shifted.",
            affectedQuantity = 100,
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun sameStatusTransition_returnsError() {
        val defect = buildDefect(DefectStatus.OPEN)
        val result = ProductionDefectLifecycleValidator.validateStatusTransition(defect, DefectStatus.OPEN)
        assertTrue(result is DomainResult.Error)
        assertEquals("Defect is already in 'Open' status.", (result as DomainResult.Error).message)
    }

    @Test
    fun terminalStatusTransitions_alwaysRejected() {
        val closedDefect = buildDefect(DefectStatus.CLOSED)
        for (target in DefectStatus.entries) {
            val result = ProductionDefectLifecycleValidator.validateStatusTransition(closedDefect, target)
            assertTrue(result is DomainResult.Error)
        }

        val cancelledDefect = buildDefect(DefectStatus.CANCELLED)
        for (target in DefectStatus.entries) {
            val result = ProductionDefectLifecycleValidator.validateStatusTransition(cancelledDefect, target)
            assertTrue(result is DomainResult.Error)
        }
    }
}
