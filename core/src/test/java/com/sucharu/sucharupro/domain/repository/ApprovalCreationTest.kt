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
import com.sucharu.sucharupro.domain.model.design.ApprovalStatus
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Approval Request Creation and Validation (Module 05 Step 04).
 */
class ApprovalCreationTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val versionV1 = DesignProofVersion(
        versionId = "prf-ver-1",
        proofId = "prf-01",
        versionNumber = 1,
        versionTag = "V1",
        artworkVersionId = "art-ver-1",
        fileReference = fileV1,
        status = ProofStatus.READY_FOR_REVIEW,
        notes = "Original V1",
        createdAt = "2026-08-16T10:00:00Z",
        createdBy = "designer-01"
    )

    private val sampleProof = DesignProof(
        proofId = "prf-01",
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        title = "প্রধান প্রচ্ছদ প্রুফ",
        status = ProofStatus.READY_FOR_REVIEW,
        currentVersionNumber = 1,
        versions = listOf(versionV1),
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
            initialProofs = listOf(sampleProof),
            initialVersions = listOf(versionV1)
        )
        proofRepository = DesignProofRepositoryImpl(proofDs, artRepo)

        approvalDataSource = FakeDesignApprovalDataSource()
        approvalRepository = DesignApprovalRepositoryImpl(approvalDataSource, proofRepository)
    }

    @Test
    fun createApprovalRequest_validProofVersion_createsPendingReview() = runBlocking {
        val result = approvalRepository.createApprovalRequest(
            proofId = "prf-01",
            targetVersionNumber = 1,
            comments = "কাস্টমার রিভিউয়ের জন্য প্রস্তুত।",
            requestedBy = "designer-01",
            requestedByName = "ডিজাইনার রিয়াদ",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val approval = (result as DomainResult.Success).data
        assertNotNull(approval.approvalId)
        assertEquals("des-01", approval.projectId)
        assertEquals("art-01", approval.artworkId)
        assertEquals("prf-01", approval.proofId)
        assertEquals("prf-ver-1", approval.proofVersionId)
        assertEquals("art-ver-1", approval.artworkVersionId)
        assertEquals(1, approval.targetProofVersionNumber)
        assertEquals(ApprovalStatus.PENDING_REVIEW, approval.status)
        assertEquals("designer-01", approval.requestedBy)

        val all = approvalRepository.observeApprovals().first()
        assertEquals(1, all.size)
    }

    @Test
    fun createApprovalRequest_invalidProofVersion_fails() = runBlocking {
        val result = approvalRepository.createApprovalRequest(
            proofId = "prf-01",
            targetVersionNumber = 99,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Target proof version V99 not found"))
    }

    @Test
    fun duplicateActiveApprovalRequest_fails() = runBlocking {
        val res1 = approvalRepository.createApprovalRequest(
            proofId = "prf-01",
            targetVersionNumber = 1,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(res1 is DomainResult.Success)

        val res2 = approvalRepository.createApprovalRequest(
            proofId = "prf-01",
            targetVersionNumber = 1,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(res2 is DomainResult.Error)
        val error = res2 as DomainResult.Error
        assertTrue(error.message.contains("already pending"))
    }
}
