package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.validation.DesignProductionHandoffValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category F: Final Lock Immutability verification during Handoff (Module 05 Step 05).
 */
class ProductionHandoffFinalLockTest {

    private val file = FileReference("f1", "cover.pdf", "application/pdf", 1000L, "/files/cover.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val project = DesignProject("des-01", "DES-2026-0001", "job-01", "ord-1", "ORD-1", "cus-1", "Project 1", DesignStatus.IN_DESIGN, createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val artwork = DesignArtwork("art-01", "des-01", "job-01", "Artwork 1", versions = emptyList(), createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val artVersion = DesignArtworkVersion("art-ver-1", "art-01", 1, "V1", file, createdAt = "2026-08-16T10:00:00Z")

    private val proof = DesignProof("prf-01", "art-01", "des-01", "job-01", "Proof 1", ProofStatus.READY_FOR_REVIEW, versions = emptyList(), createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val proofVersion = DesignProofVersion("prf-ver-1", "prf-01", 1, "V1", "art-ver-1", file, createdAt = "2026-08-16T10:00:00Z")

    @Test
    fun unfinalizedApproval_strictlyBlocksHandoff() {
        val nonFinalApproval = DesignApproval(
            approvalId = "appr-01",
            projectId = "des-01",
            artworkId = "art-01",
            proofId = "prf-01",
            proofVersionId = "prf-ver-1",
            artworkVersionId = "art-ver-1",
            targetProofVersionNumber = 1,
            status = ApprovalStatus.APPROVED, // Approved but NOT final locked
            isFinalLocked = false,
            requestedBy = "designer-01",
            requestedAt = "2026-08-16T10:00:00Z",
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val result = DesignProductionHandoffValidator.validateProductionHandoff(
            project = project,
            artwork = artwork,
            artworkVersion = artVersion,
            proof = proof,
            proofVersion = proofVersion,
            approval = nonFinalApproval,
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("not Final Locked"))
    }
}
