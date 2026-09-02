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
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Category C: Revision Integration Tests (Module 05 Step 05).
 */
class ProductionHandoffRevisionIntegrationTest {

    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkRepository: DesignArtworkRepository
    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalRepository: DesignApprovalRepository
    private lateinit var handoffRepository: DesignProductionHandoffRepository

    private val fileV1 = FileReference("f1", "cover_v1.pdf", "application/pdf", 1000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "cover_v2.pdf", "application/pdf", 1100L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

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
    fun revisionCycle_thenApproval_thenHandoffSucceeds() = runBlocking {
        // 1. Create Artwork
        val artRes = artworkRepository.createArtwork(
            projectId = "des-01",
            name = "Cover Artwork",
            initialFile = fileV1,
            description = "Initial artwork",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        val artwork = (artRes as DomainResult.Success).data
        val artV1Id = artwork.versions.first().versionId

        // 2. Create Proof V1
        val proofRes = proofRepository.createProof(
            artworkId = artwork.artworkId,
            title = "Cover Proof",
            initialArtworkVersionId = artV1Id,
            initialFile = fileV1,
            notes = "Initial proof",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:05:00Z",
            callerRole = UserRole.DESIGNER
        )
        val proof = (proofRes as DomainResult.Success).data

        // 3. Request Approval for V1
        val app1Res = approvalRepository.createApprovalRequest(
            proofId = proof.proofId,
            targetVersionNumber = 1,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:10:00Z",
            callerRole = UserRole.DESIGNER
        )
        val approval1 = (app1Res as DomainResult.Success).data

        // 4. Reviewer requests revision
        approvalRepository.requestRevision(
            approvalId = approval1.approvalId,
            reason = RevisionReason.COLOR_CHANGE,
            comments = "কালার টোন বেশি উজ্জ্বল।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T10:20:00Z",
            callerRole = UserRole.MANAGER
        )

        // Attempting handoff on approval1 must fail
        val blockCheck = handoffRepository.canHandoffToProduction(approval1.approvalId, callerRole = UserRole.MANAGER)
        assertTrue(blockCheck is DomainResult.Error)

        // 5. Designer creates Artwork V2 & Resubmits Proof V2
        val artV2Res = artworkRepository.createArtworkVersion(
            artworkId = artwork.artworkId,
            fileReference = fileV2,
            notes = "কালার কারেকশন করা হয়েছে",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )
        val artV2Id = (artV2Res as DomainResult.Success).data.versionId

        proofRepository.startRevision(proofId = proof.proofId, actorId = "designer-01", timestamp = "2026-08-16T10:35:00Z", callerRole = UserRole.DESIGNER)
        proofRepository.resubmitProof(
            proofId = proof.proofId,
            artworkVersionId = artV2Id,
            fileReference = fileV2,
            notes = "কালার টোন অ্যাডজাস্ট করা হয়েছে।",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:40:00Z",
            callerRole = UserRole.DESIGNER
        )

        // 6. Request Approval for V2
        val app2Res = approvalRepository.createApprovalRequest(
            proofId = proof.proofId,
            targetVersionNumber = 2,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:45:00Z",
            callerRole = UserRole.DESIGNER
        )
        val approval2 = (app2Res as DomainResult.Success).data

        // 7. Manager Approves V2 and Final Locks
        approvalRepository.approve(
            approvalId = approval2.approvalId,
            comments = "অনুমোদিত",
            reviewerId = "mgr-01",
            timestamp = "2026-08-16T10:50:00Z",
            callerRole = UserRole.MANAGER
        )
        approvalRepository.lockFinalApproval(
            approvalId = approval2.approvalId,
            lockedBy = "mgr-01",
            timestamp = "2026-08-16T10:55:00Z",
            callerRole = UserRole.MANAGER
        )

        // 8. Authorize Production Handoff
        val handoffRes = handoffRepository.authorizeProductionHandoff(
            approvalId = approval2.approvalId,
            authorizedBy = "mgr-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data
        assertEquals(artV2Id, handoff.artworkVersionId)
    }
}
