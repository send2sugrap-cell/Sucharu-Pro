package com.sucharu.sucharupro.domain.validation

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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Category E: Cross-Project Isolation Tests for Production Handoff (Module 05 Step 05).
 */
class ProductionHandoffCrossProjectTest {

    private val file = FileReference("f1", "cover.pdf", "application/pdf", 1000L, "/files/cover.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val projectA = DesignProject("des-A", "DES-2026-0001", "job-A", "ord-1", "ORD-1", "cus-1", "Project A", DesignStatus.IN_DESIGN, createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val projectB = DesignProject("des-B", "DES-2026-0002", "job-B", "ord-2", "ORD-2", "cus-2", "Project B", DesignStatus.IN_DESIGN, createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")

    private val artworkA = DesignArtwork("art-A", "des-A", "job-A", "Artwork A", versions = emptyList(), createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val artVersionA = DesignArtworkVersion("art-ver-A", "art-A", 1, "V1", file, createdAt = "2026-08-16T10:00:00Z")

    private val proofA = DesignProof("prf-A", "art-A", "des-A", "job-A", "Proof A", ProofStatus.READY_FOR_REVIEW, versions = emptyList(), createdAt = "2026-08-16T10:00:00Z", updatedAt = "2026-08-16T10:00:00Z")
    private val proofVersionA = DesignProofVersion("prf-ver-A", "prf-A", 1, "V1", "art-ver-A", file, createdAt = "2026-08-16T10:00:00Z")

    private val approvalA = DesignApproval(
        approvalId = "appr-A",
        projectId = "des-A",
        artworkId = "art-A",
        proofId = "prf-A",
        proofVersionId = "prf-ver-A",
        artworkVersionId = "art-ver-A",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.FINAL_LOCKED,
        isFinalLocked = true,
        finalApprovedProofVersionId = "prf-ver-A",
        finalApprovedArtworkVersionId = "art-ver-A",
        lockedAt = "2026-08-16T10:30:00Z",
        lockedBy = "mgr-01",
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T10:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:30:00Z"
    )

    @Test
    fun crossProjectHandoffAttempt_failsValidation() {
        // Attempting to hand off Project B using Project A's artwork/proof/approval
        val result = DesignProductionHandoffValidator.validateProductionHandoff(
            project = projectB,
            artwork = artworkA,
            artworkVersion = artVersionA,
            proof = proofA,
            proofVersion = proofVersionA,
            approval = approvalA,
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Cross-project mismatch"))
    }
}
