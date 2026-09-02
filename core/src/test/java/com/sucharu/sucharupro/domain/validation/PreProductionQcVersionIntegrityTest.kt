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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for exact artwork, proof, and final approval version alignment in Pre-Production QC (Module 06 Step 02).
 */
class PreProductionQcVersionIntegrityTest {

    private val fileRef = FileReference("f1", "file.pdf", "application/pdf", 1000L, "/path/file.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val sampleProject = DesignProject(
        projectId = "proj-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-1",
        orderNumber = "ORD-1",
        customerId = "cus-1",
        title = "Test Project",
        status = DesignStatus.APPROVED,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val artVersion1 = DesignArtworkVersion(
        versionId = "av-1",
        artworkId = "art-01",
        versionNumber = 1,
        fileReference = fileRef,
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleArtwork = DesignArtwork(
        artworkId = "art-01",
        projectId = "proj-01",
        productionJobId = "job-01",
        name = "Artwork 1",
        versions = listOf(artVersion1),
        currentVersionNumber = 1,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val proofVersion1 = DesignProofVersion(
        versionId = "pv-1",
        proofId = "proof-01",
        versionNumber = 1,
        artworkVersionId = "av-1",
        fileReference = fileRef,
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleProof = DesignProof(
        proofId = "proof-01",
        artworkId = "art-01",
        projectId = "proj-01",
        productionJobId = "job-01",
        title = "Proof 1",
        versions = listOf(proofVersion1),
        currentVersionNumber = 1,
        status = ProofStatus.READY_FOR_REVIEW,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleApproval = DesignApproval(
        approvalId = "app-01",
        projectId = "proj-01",
        artworkId = "art-01",
        proofId = "proof-01",
        proofVersionId = "pv-1",
        artworkVersionId = "av-1",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.FINAL_LOCKED,
        isFinalLocked = true,
        finalApprovedProofVersionId = "pv-1",
        finalApprovedArtworkVersionId = "av-1",
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T10:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun versionIntegrity_matchingVersions_passes() {
        val res = PreProductionQcValidator.validateVersionIntegrity(
            project = sampleProject,
            artwork = sampleArtwork,
            inspectedArtworkVersionId = "av-1",
            proof = sampleProof,
            inspectedProofVersionId = "pv-1",
            approval = sampleApproval,
            productionJobId = "job-01"
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun versionIntegrity_mismatchedArtworkVersion_fails() {
        val res = PreProductionQcValidator.validateVersionIntegrity(
            project = sampleProject,
            artwork = sampleArtwork,
            inspectedArtworkVersionId = "av-999", // Unapproved/mismatched version
            proof = sampleProof,
            inspectedProofVersionId = "pv-1",
            approval = sampleApproval,
            productionJobId = "job-01"
        )
        assertTrue(res is DomainResult.Error)
    }
}
