package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying valid and invalid status transitions in the Proof Lifecycle (Module 05 Step 03).
 */
class ProofLifecycleTest {

    private fun buildProof(status: ProofStatus): DesignProof {
        return DesignProof(
            proofId = "prf-01",
            artworkId = "art-01",
            projectId = "des-01",
            productionJobId = "job-01",
            title = "Test Proof",
            status = status,
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )
    }

    @Test
    fun validTransitions_passValidation() {
        // DRAFT -> READY_FOR_REVIEW
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.DRAFT), ProofStatus.READY_FOR_REVIEW) is DomainResult.Success)

        // READY_FOR_REVIEW -> REVISION_REQUESTED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.READY_FOR_REVIEW), ProofStatus.REVISION_REQUESTED) is DomainResult.Success)

        // REVISION_REQUESTED -> REVISING
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.REVISION_REQUESTED), ProofStatus.REVISING) is DomainResult.Success)

        // REVISING -> RESUBMITTED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.REVISING), ProofStatus.RESUBMITTED) is DomainResult.Success)

        // RESUBMITTED -> REVISION_REQUESTED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.RESUBMITTED), ProofStatus.REVISION_REQUESTED) is DomainResult.Success)

        // Any non-terminal state -> ARCHIVED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.READY_FOR_REVIEW), ProofStatus.ARCHIVED) is DomainResult.Success)
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.RESUBMITTED), ProofStatus.ARCHIVED) is DomainResult.Success)
    }

    @Test
    fun invalidTransitions_failValidation() {
        // DRAFT cannot directly jump to REVISING or RESUBMITTED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.DRAFT), ProofStatus.REVISING) is DomainResult.Error)
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.DRAFT), ProofStatus.RESUBMITTED) is DomainResult.Error)

        // READY_FOR_REVIEW cannot jump directly to RESUBMITTED
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.READY_FOR_REVIEW), ProofStatus.RESUBMITTED) is DomainResult.Error)

        // ARCHIVED cannot transition to anything
        assertTrue(DesignProofValidator.validateStatusTransition(buildProof(ProofStatus.ARCHIVED), ProofStatus.READY_FOR_REVIEW) is DomainResult.Error)
    }
}
