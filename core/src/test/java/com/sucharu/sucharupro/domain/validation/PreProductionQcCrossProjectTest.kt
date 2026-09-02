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
 * Cross-project and cross-job integrity tests for Pre-Production QC (Module 06 Step 02).
 */
class PreProductionQcCrossProjectTest {

    private val fileRef = FileReference("f1", "file.pdf", "application/pdf", 1000L, "/path/file.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val projectA = DesignProject(
        projectId = "proj-A",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-A",
        orderId = "ord-A",
        orderNumber = "ORD-A",
        customerId = "cus-A",
        title = "Job A Design",
        status = DesignStatus.APPROVED,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val artworkB = DesignArtwork(
        artworkId = "art-B",
        projectId = "proj-B", // Belongs to Project B!
        productionJobId = "job-B",
        name = "Artwork B",
        versions = listOf(
            DesignArtworkVersion(
                versionId = "av-B1",
                artworkId = "art-B",
                versionNumber = 1,
                fileReference = fileRef,
                createdAt = "2026-08-16T10:00:00Z"
            )
        ),
        currentVersionNumber = 1,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val proofB = DesignProof(
        proofId = "proof-B",
        artworkId = "art-B",
        projectId = "proj-B",
        productionJobId = "job-B",
        title = "Proof B",
        versions = listOf(
            DesignProofVersion(
                versionId = "pv-B1",
                proofId = "proof-B",
                versionNumber = 1,
                artworkVersionId = "av-B1",
                fileReference = fileRef,
                createdAt = "2026-08-16T10:00:00Z"
            )
        ),
        currentVersionNumber = 1,
        status = ProofStatus.READY_FOR_REVIEW,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val approvalB = DesignApproval(
        approvalId = "app-B",
        projectId = "proj-B",
        artworkId = "art-B",
        proofId = "proof-B",
        proofVersionId = "pv-B1",
        artworkVersionId = "av-B1",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.FINAL_LOCKED,
        isFinalLocked = true,
        finalApprovedProofVersionId = "pv-B1",
        finalApprovedArtworkVersionId = "av-B1",
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T10:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun crossProjectArtwork_failsValidation() {
        val res = PreProductionQcValidator.validateVersionIntegrity(
            project = projectA,
            artwork = artworkB, // Cross-project artwork!
            inspectedArtworkVersionId = "av-B1",
            proof = proofB,
            inspectedProofVersionId = "pv-B1",
            approval = approvalB,
            productionJobId = "job-A"
        )

        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("does not belong to Project"))
    }
}
