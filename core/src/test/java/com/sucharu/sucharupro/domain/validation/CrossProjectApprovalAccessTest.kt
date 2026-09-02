package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests ensuring cross-project approval access integrity (Module 05 Step 04).
 */
class CrossProjectApprovalAccessTest {

    @Test
    fun approvalRequest_requiresTargetVersionBelongingToProof() {
        val proof = DesignProof(
            proofId = "prf-proj-1",
            artworkId = "art-proj-1",
            projectId = "des-proj-1",
            productionJobId = "job-1",
            title = "Proof 1",
            status = ProofStatus.READY_FOR_REVIEW,
            versions = listOf(
                DesignProofVersion(
                    versionId = "ver-1",
                    proofId = "prf-proj-1",
                    versionNumber = 1,
                    versionTag = "V1",
                    artworkVersionId = "art-ver-1",
                    fileReference = FileReference("f1", "p.pdf", "application/pdf", 1000L, "/files/p.pdf", uploadedAt = "2026-08-16T10:00:00Z"),
                    createdAt = "2026-08-16T10:00:00Z"
                )
            ),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        // Attempting to request approval on version 2 which does not exist in this proof
        val result = DesignApprovalValidator.validateApprovalCreation(
            proof = proof,
            targetVersionNumber = 2,
            existingApprovals = emptyList(),
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Target proof version V2 not found"))
    }
}
