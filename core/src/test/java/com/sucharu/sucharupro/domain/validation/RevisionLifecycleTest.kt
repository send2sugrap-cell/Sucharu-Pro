package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignRevisionRequest
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests verifying revision lifecycle stages: OPEN -> IN_PROGRESS -> RESOLVED (Module 05 Step 03).
 */
class RevisionLifecycleTest {

    private val sampleVersion = DesignProofVersion(
        versionId = "v-1",
        proofId = "p-1",
        versionNumber = 1,
        versionTag = "V1",
        artworkVersionId = "art-v-1",
        fileReference = FileReference("f1", "doc.pdf", "application/pdf", 1000L, "/files/doc.pdf", uploadedAt = "2026-08-16T10:00:00Z"),
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleRevision = DesignRevisionRequest(
        requestId = "rev-1",
        proofId = "p-1",
        proofVersionId = "v-1",
        targetVersionNumber = 1,
        reason = RevisionReason.CONTENT_CORRECTION,
        notes = "বানান ও ব্যাকরণ সংশোধন",
        status = RevisionRequestStatus.OPEN,
        requestedBy = "mgr-1",
        requestedAt = "2026-08-16T10:00:00Z"
    )

    @Test
    fun startRevisionValidation_verifiesStatusAndActiveRequest() {
        val proofReady = DesignProof(
            proofId = "p-1",
            artworkId = "art-1",
            projectId = "des-1",
            productionJobId = "job-1",
            title = "Proof",
            status = ProofStatus.REVISION_REQUESTED,
            versions = listOf(sampleVersion),
            revisions = listOf(sampleRevision),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val result = DesignProofValidator.validateStartRevision(proofReady, UserRole.DESIGNER)
        assertTrue(result is DomainResult.Success)

        val wrongStateProof = proofReady.copy(status = ProofStatus.READY_FOR_REVIEW)
        val wrongResult = DesignProofValidator.validateStartRevision(wrongStateProof, UserRole.DESIGNER)
        assertTrue(wrongResult is DomainResult.Error)
    }

    @Test
    fun resubmitProofValidation_verifiesRevisingStatus() {
        val proofRevising = DesignProof(
            proofId = "p-1",
            artworkId = "art-1",
            projectId = "des-1",
            productionJobId = "job-1",
            title = "Proof",
            status = ProofStatus.REVISING,
            versions = listOf(sampleVersion),
            revisions = listOf(sampleRevision.copy(status = RevisionRequestStatus.IN_PROGRESS)),
            createdAt = "2026-08-16T10:00:00Z",
            updatedAt = "2026-08-16T10:00:00Z"
        )

        val result = DesignProofValidator.validateResubmitProof(proofRevising, UserRole.DESIGNER)
        assertTrue(result is DomainResult.Success)

        val wrongStateProof = proofRevising.copy(status = ProofStatus.DRAFT)
        val wrongResult = DesignProofValidator.validateResubmitProof(wrongStateProof, UserRole.DESIGNER)
        assertTrue(wrongResult is DomainResult.Error)
    }
}
