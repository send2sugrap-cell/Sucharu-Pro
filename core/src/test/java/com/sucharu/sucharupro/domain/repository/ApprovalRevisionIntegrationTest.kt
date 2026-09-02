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
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End integration test: Proof V1 -> Approval Request -> Revision Required (Step 03 integration) -> Designer Revision -> Proof V2 -> Approval -> Approved -> Final Lock (Module 05 Step 04).
 */
class ApprovalRevisionIntegrationTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1200000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

    @Before
    fun setUp() = runBlocking {
        val projDs = FakeDesignProjectDataSource(
            initialProjects = listOf(
                DesignProject(
                    projectId = "des-int-01",
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
                    projectId = "des-int-01",
                    productionJobId = "job-01",
                    name = "Artwork 1",
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            )
        )
        val artRepo = DesignArtworkRepositoryImpl(artDs, projRepo)

        proofDataSource = FakeDesignProofDataSource()
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artRepo)

        approvalDataSource = FakeDesignApprovalDataSource()
        approvalRepository = DesignApprovalRepositoryImpl(approvalDataSource, proofRepository)
    }

    @Test
    fun approvalRevisionCycle_seamlesslyIntegratesWithStep03AndFinalLocks() = runBlocking {
        // 1. Create Proof (V1 in READY_FOR_REVIEW)
        val proofRes = proofRepository.createProof(
            artworkId = "art-01",
            title = "বই কভার প্রুফ",
            initialArtworkVersionId = "art-ver-1",
            initialFile = fileV1,
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        val proofId = (proofRes as DomainResult.Success).data.proofId

        // 2. Submit Approval Request for V1
        val req1Res = approvalRepository.createApprovalRequest(
            proofId = proofId,
            targetVersionNumber = 1,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(req1Res is DomainResult.Success)
        val approval1Id = (req1Res as DomainResult.Success).data.approvalId

        // 3. Manager Reviews and Requests Revision (Integrated with Step 03)
        val revDecisionRes = approvalRepository.requestRevision(
            approvalId = approval1Id,
            reason = RevisionReason.LAYOUT_CHANGE,
            comments = "ফ্রন্ট কভারের টাইটেল ও লোগো পজিশন ২ সেমি নিচে নামাতে হবে।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(revDecisionRes is DomainResult.Success)
        val revDecision = (revDecisionRes as DomainResult.Success).data
        assertEquals(ApprovalDecisionType.REVISION_REQUIRED, revDecision.decisionType)
        assertNotNull(revDecision.revisionRequestId)

        // Verify Step 03 proof transitioned to REVISION_REQUESTED
        val proofAfterRev = (proofRepository.findProofById(proofId) as DomainResult.Success).data
        assertEquals(ProofStatus.REVISION_REQUESTED, proofAfterRev.status)
        assertEquals(1, proofAfterRev.revisions.size)

        // 4. Designer Starts Revision and Resubmits V2 (Step 03 workflow)
        proofRepository.startRevision(proofId = proofId, actorId = "designer-01", timestamp = "2026-08-16T10:45:00Z", callerRole = UserRole.DESIGNER)
        val resubmitRes = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = "art-ver-2",
            fileReference = fileV2,
            notes = "লোগো ও টাইটেল পজিশন অ্যাডজাস্ট করা হয়েছে।",
            createdBy = "designer-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(resubmitRes is DomainResult.Success)
        val versionV2 = (resubmitRes as DomainResult.Success).data
        assertEquals(2, versionV2.versionNumber)

        // 5. Submit Approval Request for V2
        val req2Res = approvalRepository.createApprovalRequest(
            proofId = proofId,
            targetVersionNumber = 2,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T11:15:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(req2Res is DomainResult.Success)
        val approval2Id = (req2Res as DomainResult.Success).data.approvalId

        // 6. Manager Approves V2
        val approveRes = approvalRepository.approve(
            approvalId = approval2Id,
            comments = "পারফেক্ট! প্রিন্টের জন্য অনুমোদন দেওয়া হলো।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T11:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(approveRes is DomainResult.Success)

        // 7. Apply Final Lock
        val lockRes = approvalRepository.lockFinalApproval(
            approvalId = approval2Id,
            lockedBy = "mgr-01",
            timestamp = "2026-08-16T11:35:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(lockRes is DomainResult.Success)
        val lockedApproval = (lockRes as DomainResult.Success).data
        assertEquals(ApprovalStatus.FINAL_LOCKED, lockedApproval.status)
        assertTrue(lockedApproval.isFinalLocked)
        assertEquals(versionV2.versionId, lockedApproval.finalApprovedProofVersionId)
        assertEquals("art-ver-2", lockedApproval.finalApprovedArtworkVersionId)
    }
}
