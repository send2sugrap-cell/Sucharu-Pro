package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ProductionDefectLifecycleValidator] lifecycle state machine transitions (Module 06 Step 04).
 */
class ProductionDefectLifecycleTest {

    private fun createDefectWithStatus(status: DefectStatus): ProductionDefect {
        return ProductionDefect(
            defectId = "def-test",
            productionJobId = "job-01",
            category = DefectCategory.LAMINATION_ERROR,
            severity = DefectSeverity.MAJOR,
            source = DefectSource.PRODUCTION_STAGE,
            status = status,
            title = "Lamination bubbles",
            description = "Thermal film bubbles appearing at sheet edges.",
            affectedQuantity = 50,
            detectedAt = "2026-08-17T10:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun validProgressiveTransitions_allSucceed() {
        val openDefect = createDefectWithStatus(DefectStatus.OPEN)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(openDefect, DefectStatus.ACKNOWLEDGED).isSuccess)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(openDefect, DefectStatus.CANCELLED).isSuccess)

        val ackDefect = createDefectWithStatus(DefectStatus.ACKNOWLEDGED)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(ackDefect, DefectStatus.UNDER_INVESTIGATION).isSuccess)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(ackDefect, DefectStatus.CONTAINED).isSuccess)

        val invDefect = createDefectWithStatus(DefectStatus.UNDER_INVESTIGATION)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(invDefect, DefectStatus.CONTAINED).isSuccess)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(invDefect, DefectStatus.RESOLUTION_PENDING).isSuccess)

        val resPendDefect = createDefectWithStatus(DefectStatus.RESOLUTION_PENDING)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(resPendDefect, DefectStatus.RESOLVED).isSuccess)

        val resDefect = createDefectWithStatus(DefectStatus.RESOLVED)
        assertTrue(ProductionDefectLifecycleValidator.validateStatusTransition(resDefect, DefectStatus.CLOSED).isSuccess)
    }

    @Test
    fun invalidTransitions_rejected() {
        val openDefect = createDefectWithStatus(DefectStatus.OPEN)
        // Cannot jump directly from OPEN to RESOLVED or CLOSED
        assertFalse(ProductionDefectLifecycleValidator.validateStatusTransition(openDefect, DefectStatus.RESOLVED).isSuccess)
        assertFalse(ProductionDefectLifecycleValidator.validateStatusTransition(openDefect, DefectStatus.CLOSED).isSuccess)

        val closedDefect = createDefectWithStatus(DefectStatus.CLOSED)
        assertFalse(ProductionDefectLifecycleValidator.validateStatusTransition(closedDefect, DefectStatus.OPEN).isSuccess)
        assertFalse(ProductionDefectLifecycleValidator.validateStatusTransition(closedDefect, DefectStatus.UNDER_INVESTIGATION).isSuccess)

        val cancelledDefect = createDefectWithStatus(DefectStatus.CANCELLED)
        assertFalse(ProductionDefectLifecycleValidator.validateStatusTransition(cancelledDefect, DefectStatus.ACKNOWLEDGED).isSuccess)
    }

    @Test
    fun cancellation_withBlankReason_fails() {
        val defect = createDefectWithStatus(DefectStatus.OPEN)
        val res = ProductionDefectLifecycleValidator.validateCancellation(defect, reason = "")
        assertTrue(res is DomainResult.Error)
    }
}
