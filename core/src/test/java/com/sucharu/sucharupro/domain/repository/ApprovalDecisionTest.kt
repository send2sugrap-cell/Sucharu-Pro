package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignApprovalDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProofDataSource
import com.sucharu.sucharupro.data.repository.DesignApprovalRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ApprovalDecisionType
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for reviewer decision handling: APPROVED, REVISION_REQUIRED, REJECTED (Module 05 Step 04).
 */
class ApprovalDecisionTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val sampleApproval = DesignApproval(
        approvalId = "appr-01",
        projectId = "des-01",
        artworkId = "art-01",
        proofId = "prf-01",
        proofVersionId = "ver-01",
        artworkVersionId = "art-ver-01",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.PENDING_REVIEW,
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T10:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        val projDs = FakeDesignProjectDataSource(
            initialProjects = listOf(
                DesignProject(
                    projectId = "des-01",
                    projectNumber = "DES-2026-0001",
                    productionJobId = "job-01",
                    orderId = "ord-01",
                    orderNumber = "ORD-2026-0001",
                    customerId = "cus-01",
                    title = "Test",
                    status = DesignStatus.IN_DESIGN,
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            )
        )
        val projRepo = DesignProjectRepositoryImpl(projDs)
        val artDs = FakeDesignArtworkDataSource(
            initialArtworks = listOf(
                DesignArtwork(
                    artworkId = "art-01",
                    projectId = "des-01",
                    productionJobId = "job-01",
                    name = "Artwork 1",
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            )
        )
        val artRepo = DesignArtworkRepositoryImpl(artDs, projRepo)

        val proofDs = FakeDesignProofDataSource(
            initialProofs = listOf(
                DesignProof(
                    proofId = "prf-01",
                    artworkId = "art-01",
                    projectId = "des-01",
                    productionJobId = "job-01",
                    title = "Proof",
                    status = ProofStatus.READY_FOR_REVIEW,
                    versions = listOf(
                        DesignProofVersion(
                            versionId = "ver-01",
                            proofId = "prf-01",
                            versionNumber = 1,
                            versionTag = "V1",
                            artworkVersionId = "art-ver-01",
                            fileReference = FileReference("f1", "p.pdf", "application/pdf", 1000L, "/files/p.pdf", uploadedAt = "2026-08-16T10:00:00Z"),
                            createdAt = "2026-08-16T10:00:00Z"
                        )
                    ),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            )
        )
        proofRepository = DesignProofRepositoryImpl(proofDs, artRepo)

        approvalDataSource = FakeDesignApprovalDataSource(initialApprovals = listOf(sampleApproval))
        approvalRepository = DesignApprovalRepositoryImpl(approvalDataSource, proofRepository)
    }

    @Test
    fun approveDecision_transitionsStatusToApprovedAndRecordsDecision() = runBlocking {
        val result = approvalRepository.approve(
            approvalId = "appr-01",
            comments = "প্রিন্টিংয়ের জন্য উপযুক্ত।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার সাহেব",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val approval = (result as DomainResult.Success).data
        assertEquals(ApprovalStatus.APPROVED, approval.status)
        assertEquals("mgr-01", approval.reviewerId)
        assertEquals(1, approval.decisions.size)
        val decision = approval.decisions.first()
        assertEquals(ApprovalDecisionType.APPROVED, decision.decisionType)
        assertEquals("প্রিন্টিংয়ের জন্য উপযুক্ত।", decision.comments)
    }

    @Test
    fun rejectDecision_transitionsStatusToRejected() = runBlocking {
        val result = approvalRepository.reject(
            approvalId = "appr-01",
            comments = "ভুল স্পেসিফিকেশন ও অস্পষ্ট ইমেজ।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার সাহেব",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val decision = (result as DomainResult.Success).data
        assertEquals(ApprovalDecisionType.REJECTED, decision.decisionType)

        val updatedApproval = (approvalRepository.findApprovalById("appr-01") as DomainResult.Success).data
        assertEquals(ApprovalStatus.REJECTED, updatedApproval.status)
    }
}
