package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignApprovalDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProductionHandoffDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProofDataSource
import com.sucharu.sucharupro.data.repository.DesignApprovalRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProductionHandoffRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ApprovalDecisionType
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Category I: Complete End-to-End Integration Scenario (Module 05 Step 05).
 * Validates the full domain flow:
 * ProductionJob -> DesignProject -> Artwork V1 -> Proof V1 -> Approval Request -> Revision Required -> Step 03 Revision Request ->
 * Artwork V2 -> Proof V2 -> Resubmit -> Approval Request -> Approved -> FINAL_LOCKED -> Production Handoff Authorization.
 */
class DesignDomainEndToEndIntegrationTest {

    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkRepository: DesignArtworkRepository
    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalRepository: DesignApprovalRepository
    private lateinit var handoffRepository: DesignProductionHandoffRepository

    private val fileV1 = FileReference("f1", "brochure_v1.pdf", "application/pdf", 1200000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "brochure_v2.pdf", "application/pdf", 1250000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

    private val sampleJob = ProductionJob(
        jobId = "job-2026-999",
        jobNumber = "JOB-2026-0999",
        orderId = "ord-101",
        orderNumber = "ORD-2026-0101",
        customerId = "cus-505",
        handoffId = "hnd-01",
        title = "ব্রোশিওর ডিজাইন ও প্রিন্টিং",
        quantity = 500,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        val projDs = FakeDesignProjectDataSource()
        projectRepository = DesignProjectRepositoryImpl(projDs)

        val artDs = FakeDesignArtworkDataSource()
        artworkRepository = DesignArtworkRepositoryImpl(artDs, projectRepository)

        val proofDs = FakeDesignProofDataSource()
        proofRepository = DesignProofRepositoryImpl(proofDs, artworkRepository)

        val approvalDs = FakeDesignApprovalDataSource()
        approvalRepository = DesignApprovalRepositoryImpl(approvalDs, proofRepository)

        val handoffDs = FakeDesignProductionHandoffDataSource()
        handoffRepository = DesignProductionHandoffRepositoryImpl(
            dataSource = handoffDs,
            projectRepository = projectRepository,
            artworkRepository = artworkRepository,
            proofRepository = proofRepository,
            approvalRepository = approvalRepository
        )
    }

    @Test
    fun completeEndToEndDesignChain_executesWithFullTraceabilityAndSafeHandoff() = runBlocking {
        // Step 1: Create Design Project referencing Production Job
        val projRes = projectRepository.createDesignProject(
            job = sampleJob,
            title = "ব্রোশিওর ডিজাইন ও প্রিন্টিং",
            notes = "প্রিমিয়াম কোয়ালিটি ৪ কালার ব্রোশিওর",
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z"
        )
        assertTrue(projRes is DomainResult.Success)
        val project = (projRes as DomainResult.Success).data
        val projectId = project.projectId

        // Step 2: Assign Designer and Start Design Work
        projectRepository.assignDesigner(projectId, "designer-01", "ডিজাইনার রিয়াদ", "admin-01", null, "2026-08-16T10:05:00Z", UserRole.MANAGER)
        projectRepository.startDesign(projectId, "designer-01", "ডিজাইনার রিয়াদ", null, "2026-08-16T10:10:00Z")

        // Step 3: Create Artwork (V1)
        val artRes = artworkRepository.createArtwork(
            projectId = projectId,
            name = "ব্রোশিওর মূল আর্টওয়ার্ক",
            initialFile = fileV1,
            description = "প্রাথমিক ড্রাফট আর্টওয়ার্ক",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(artRes is DomainResult.Success)
        val artwork = (artRes as DomainResult.Success).data
        val artV1Id = artwork.versions.first().versionId

        // Step 4: Create Proof (V1)
        val proofRes = proofRepository.createProof(
            artworkId = artwork.artworkId,
            title = "ব্রোশিওর ক্লায়েন্ট প্রুফ",
            initialArtworkVersionId = artV1Id,
            initialFile = fileV1,
            notes = "প্রথম প্রুফ কপি",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:20:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(proofRes is DomainResult.Success)
        val proof = (proofRes as DomainResult.Success).data
        val proofId = proof.proofId
        val proofV1Id = proof.versions.first().versionId

        // Step 5: Submit Approval Request for V1
        val app1Res = approvalRepository.createApprovalRequest(
            proofId = proofId,
            targetVersionNumber = 1,
            comments = "প্রথম প্রুফ পর্যালোচনার জন্য জমা দেওয়া হলো।",
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:25:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(app1Res is DomainResult.Success)
        val approval1 = (app1Res as DomainResult.Success).data

        // Step 6: Reviewer reviews and requests Revision
        approvalRepository.startReview(approval1.approvalId, "mgr-01", "ম্যানেজার", "2026-08-16T10:30:00Z", UserRole.MANAGER)
        val revDecisionRes = approvalRepository.requestRevision(
            approvalId = approval1.approvalId,
            reason = RevisionReason.TEXT_CHANGE,
            comments = "পেজ ৩ এর অ্যাড্রেস ও ফোন নম্বর সংশোধন করতে হবে।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(revDecisionRes is DomainResult.Success)

        // Verify handoff is BLOCKED at this stage
        val blockCheck = handoffRepository.canHandoffToProduction(approval1.approvalId, UserRole.MANAGER)
        assertTrue(blockCheck is DomainResult.Error)

        // Step 7: Designer revises -> Creates Artwork V2 -> Starts Revision -> Resubmits Proof V2
        val artV2Res = artworkRepository.createArtworkVersion(
            artworkId = artwork.artworkId,
            fileReference = fileV2,
            notes = "অ্যাড্রেস সংশোধন করা হয়েছে।",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:45:00Z",
            callerRole = UserRole.DESIGNER
        )
        val artV2Id = (artV2Res as DomainResult.Success).data.versionId

        proofRepository.startRevision(proofId, "designer-01", "2026-08-16T10:50:00Z", UserRole.DESIGNER)
        val proofV2Res = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = artV2Id,
            fileReference = fileV2,
            notes = "সংশোধিত দ্বিতীয় প্রুফ",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:55:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(proofV2Res is DomainResult.Success)
        val proofV2Id = (proofV2Res as DomainResult.Success).data.versionId

        // Step 8: Submit Approval Request for V2
        val app2Res = approvalRepository.createApprovalRequest(
            proofId = proofId,
            targetVersionNumber = 2,
            comments = "সংশোধিত প্রুফ V2 পর্যালোচনার জন্য জমা দেওয়া হলো।",
            requestedBy = "designer-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(app2Res is DomainResult.Success)
        val approval2 = (app2Res as DomainResult.Success).data

        // Step 9: Reviewer Approves V2 and Final Locks
        val approveRes = approvalRepository.approve(
            approvalId = approval2.approvalId,
            comments = "সব তথ্য নির্ভুল। প্রিন্টের জন্য চূড়ান্ত অনুমোদন দেওয়া হলো।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T11:10:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(approveRes is DomainResult.Success)

        val lockRes = approvalRepository.lockFinalApproval(
            approvalId = approval2.approvalId,
            lockedBy = "mgr-01",
            timestamp = "2026-08-16T11:15:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(lockRes is DomainResult.Success)
        val lockedApproval = (lockRes as DomainResult.Success).data
        assertEquals(ApprovalStatus.FINAL_LOCKED, lockedApproval.status)
        assertTrue(lockedApproval.isFinalLocked)

        // Step 10: Authorize Production Handoff
        val canHandoff = handoffRepository.canHandoffToProduction(approval2.approvalId, UserRole.MANAGER)
        assertTrue(canHandoff is DomainResult.Success)
        assertTrue((canHandoff as DomainResult.Success).data)

        val handoffRes = handoffRepository.authorizeProductionHandoff(
            approvalId = approval2.approvalId,
            authorizedBy = "mgr-01",
            authorizedByName = "ম্যানেজার",
            notes = "Plate making authorized for Production Job job-2026-999.",
            timestamp = "2026-08-16T11:20:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data

        // Step 11: Validate All Traceability Links
        assertEquals(projectId, handoff.projectId)
        assertEquals("job-2026-999", handoff.productionJobId)
        assertEquals(artwork.artworkId, handoff.artworkId)
        assertEquals(artV2Id, handoff.artworkVersionId)
        assertEquals(proofId, handoff.proofId)
        assertEquals(proofV2Id, handoff.proofVersionId)
        assertEquals(approval2.approvalId, handoff.approvalId)
        assertEquals("mgr-01", handoff.authorizedBy)

        // Step 12: Validate Reactive Streams
        val projectHandoffs = handoffRepository.getHandoffForProject(projectId).first()
        assertEquals(1, projectHandoffs.size)
        assertEquals(handoff.handoffId, projectHandoffs.first().handoffId)

        val jobHandoffs = handoffRepository.getHandoffForJob("job-2026-999").first()
        assertEquals(1, jobHandoffs.size)
        assertEquals(handoff.handoffId, jobHandoffs.first().handoffId)
    }
}
