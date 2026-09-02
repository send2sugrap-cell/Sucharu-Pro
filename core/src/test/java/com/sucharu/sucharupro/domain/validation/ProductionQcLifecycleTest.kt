package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Production QC Lifecycle State Transitions and Terminal State Protection (Module 06 Step 01).
 */
class ProductionQcLifecycleTest {

    private fun buildQc(status: QcStatus, inspectorId: String? = null): ProductionQc {
        return ProductionQc(
            qcId = "qc-01",
            productionJobId = "job-01",
            qcType = QcType.PRE_PRODUCTION,
            status = status,
            assignedInspectorId = inspectorId,
            assignedInspectorName = inspectorId?.let { "Inspector $it" },
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun validTransitions_passValidation() {
        // DRAFT -> PENDING_INSPECTION
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.DRAFT), QcStatus.PENDING_INSPECTION) is DomainResult.Success)

        // PENDING_INSPECTION -> IN_INSPECTION (with assigned inspector)
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.PENDING_INSPECTION, "insp-01"), QcStatus.IN_INSPECTION) is DomainResult.Success)

        // IN_INSPECTION -> PASSED
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.IN_INSPECTION, "insp-01"), QcStatus.PASSED) is DomainResult.Success)

        // IN_INSPECTION -> FAILED
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.IN_INSPECTION, "insp-01"), QcStatus.FAILED) is DomainResult.Success)

        // DRAFT -> CANCELLED
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.DRAFT), QcStatus.CANCELLED) is DomainResult.Success)
    }

    @Test
    fun invalidTransitions_failValidation() {
        // PENDING_INSPECTION -> IN_INSPECTION without assigned inspector fails
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.PENDING_INSPECTION, null), QcStatus.IN_INSPECTION) is DomainResult.Error)

        // PASSED is terminal and cannot transition
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.PASSED), QcStatus.IN_INSPECTION) is DomainResult.Error)

        // FAILED is terminal and cannot transition
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.FAILED), QcStatus.DRAFT) is DomainResult.Error)

        // CANCELLED is terminal and cannot transition
        assertTrue(ProductionQcLifecycleValidator.validateStatusTransition(buildQc(QcStatus.CANCELLED), QcStatus.PENDING_INSPECTION) is DomainResult.Error)
    }
}
