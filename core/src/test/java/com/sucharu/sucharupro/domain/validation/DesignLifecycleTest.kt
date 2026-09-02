package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DesignLifecycleValidator] and [DesignStatus] state machine (Module 05 Step 01).
 */
class DesignLifecycleTest {

    private val baseProject = DesignProject(
        projectId = "des-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.NOT_STARTED,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun validTransitions_passValidation() {
        // NOT_STARTED -> ASSIGNED
        val p1 = baseProject.copy(status = DesignStatus.NOT_STARTED)
        val res1 = DesignLifecycleValidator.validateStatusTransition(p1, DesignStatus.ASSIGNED)
        assertTrue(res1 is DomainResult.Success)

        // ASSIGNED -> IN_DESIGN (with designer assigned)
        val p2 = baseProject.copy(status = DesignStatus.ASSIGNED, assignedDesignerId = "des-01", assignedDesignerName = "Tanveer")
        val res2 = DesignLifecycleValidator.validateStatusTransition(p2, DesignStatus.IN_DESIGN)
        assertTrue(res2 is DomainResult.Success)

        // IN_DESIGN -> PROOF_PENDING
        val p3 = p2.copy(status = DesignStatus.IN_DESIGN)
        val res3 = DesignLifecycleValidator.validateStatusTransition(p3, DesignStatus.PROOF_PENDING)
        assertTrue(res3 is DomainResult.Success)

        // PROOF_PENDING -> CUSTOMER_REVIEW
        val p4 = p3.copy(status = DesignStatus.PROOF_PENDING)
        val res4 = DesignLifecycleValidator.validateStatusTransition(p4, DesignStatus.CUSTOMER_REVIEW)
        assertTrue(res4 is DomainResult.Success)
    }

    @Test
    fun selfTransition_failsValidation() {
        val project = baseProject.copy(status = DesignStatus.ASSIGNED)
        val res = DesignLifecycleValidator.validateStatusTransition(project, DesignStatus.ASSIGNED)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("already in Assigned state"))
    }

    @Test
    fun inDesign_withoutDesigner_failsValidation() {
        val project = baseProject.copy(status = DesignStatus.ASSIGNED, assignedDesignerId = null)
        val res = DesignLifecycleValidator.validateStatusTransition(project, DesignStatus.IN_DESIGN)
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("without an assigned designer"))
    }

    @Test
    fun terminalStatus_cannotTransition() {
        val cancelled = baseProject.copy(status = DesignStatus.CANCELLED)
        val res1 = DesignLifecycleValidator.validateStatusTransition(cancelled, DesignStatus.IN_DESIGN)
        assertTrue(res1 is DomainResult.Error)

        val handedOff = baseProject.copy(status = DesignStatus.HANDED_OFF_TO_PRODUCTION)
        val res2 = DesignLifecycleValidator.validateStatusTransition(handedOff, DesignStatus.IN_DESIGN)
        assertTrue(res2 is DomainResult.Error)
    }

    @Test
    fun cancellation_withBlankReason_failsValidation() {
        val project = baseProject.copy(status = DesignStatus.ASSIGNED)
        val res = DesignLifecycleValidator.validateCancellation(project, "  ")
        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Cancellation reason is required"))
    }

    @Test
    fun cancellation_withValidReason_passesValidation() {
        val project = baseProject.copy(status = DesignStatus.ASSIGNED)
        val res = DesignLifecycleValidator.validateCancellation(project, "Customer cancelled publication")
        assertTrue(res is DomainResult.Success)
    }
}
