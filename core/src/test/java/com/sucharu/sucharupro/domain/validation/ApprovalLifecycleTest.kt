package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Approval Lifecycle Status Transitions (Module 05 Step 04).
 */
class ApprovalLifecycleTest {

    private fun buildApproval(status: ApprovalStatus, isFinalLocked: Boolean = false): DesignApproval {
        return DesignApproval(
            approvalId = "appr-01",
            projectId = "des-01",
            artworkId = "art-01",
            proofId = "prf-01",
            proofVersionId = "ver-01",
            artworkVersionId = "art-ver-01",
            targetProofVersionNumber = 1,
            status = status,
            isFinalLocked = isFinalLocked,
            requestedBy = "designer-01",
            requestedAt = "2026-08-16T10:00:00Z",
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun validTransitions_passValidation() {
        // PENDING_REVIEW -> UNDER_REVIEW
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.PENDING_REVIEW), ApprovalStatus.UNDER_REVIEW) is DomainResult.Success)

        // UNDER_REVIEW -> APPROVED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.UNDER_REVIEW), ApprovalStatus.APPROVED) is DomainResult.Success)

        // UNDER_REVIEW -> REVISION_REQUIRED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.UNDER_REVIEW), ApprovalStatus.REVISION_REQUIRED) is DomainResult.Success)

        // UNDER_REVIEW -> REJECTED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.UNDER_REVIEW), ApprovalStatus.REJECTED) is DomainResult.Success)

        // APPROVED -> FINAL_LOCKED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.APPROVED), ApprovalStatus.FINAL_LOCKED) is DomainResult.Success)

        // REVISION_REQUIRED -> RESUBMITTED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.REVISION_REQUIRED), ApprovalStatus.RESUBMITTED) is DomainResult.Success)

        // RESUBMITTED -> PENDING_REVIEW
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.RESUBMITTED), ApprovalStatus.PENDING_REVIEW) is DomainResult.Success)
    }

    @Test
    fun invalidTransitions_failValidation() {
        // FINAL_LOCKED cannot transition to any status
        val locked = buildApproval(ApprovalStatus.FINAL_LOCKED, isFinalLocked = true)
        assertTrue(DesignApprovalValidator.validateStatusTransition(locked, ApprovalStatus.APPROVED) is DomainResult.Error)
        assertTrue(DesignApprovalValidator.validateStatusTransition(locked, ApprovalStatus.PENDING_REVIEW) is DomainResult.Error)

        // REJECTED cannot transition
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.REJECTED), ApprovalStatus.PENDING_REVIEW) is DomainResult.Error)

        // PENDING_REVIEW cannot jump directly to FINAL_LOCKED
        assertTrue(DesignApprovalValidator.validateStatusTransition(buildApproval(ApprovalStatus.PENDING_REVIEW), ApprovalStatus.FINAL_LOCKED) is DomainResult.Error)
    }
}
