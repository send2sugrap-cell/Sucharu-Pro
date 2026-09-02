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
import com.sucharu.sucharupro.domain.model.design.DesignApproval
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignProof
import com.sucharu.sucharupro.domain.model.design.DesignProofVersion
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying that approvals are strictly bound to their targeted ProofVersion and do not silently switch versions (Module 05 Step 04).
 */
class StaleApprovalTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1100000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

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
                            fileReference = fileV1,
                            createdAt = "2026-08-16T10:00:00Z"
                        ),
                        DesignProofVersion(
                            versionId = "ver-02",
                            proofId = "prf-01",
                            versionNumber = 2,
                            versionTag = "V2",
                            artworkVersionId = "art-ver-02",
                            fileReference = fileV2,
                            createdAt = "2026-08-16T11:00:00Z"
                        )
                    ),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T11:00:00Z"
                )
            )
        )
        proofRepository = DesignProofRepositoryImpl(proofDs, artRepo)

        approvalDataSource = FakeDesignApprovalDataSource()
        approvalRepository = DesignApprovalRepositoryImpl(approvalDataSource, proofRepository)
    }

    @Test
    fun approvalRequest_isStrictlyBoundToSubmittedVersion() = runBlocking {
        val reqRes = approvalRepository.createApprovalRequest(
            proofId = "prf-01",
            targetVersionNumber = 1,
            requestedBy = "designer-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(reqRes is DomainResult.Success)
        val approval = (reqRes as DomainResult.Success).data

        assertEquals(1, approval.targetProofVersionNumber)
        assertEquals("ver-01", approval.proofVersionId)
        assertEquals("art-ver-01", approval.artworkVersionId)

        // Approving V1 does not alter or approve V2
        val approveRes = approvalRepository.approve(
            approvalId = approval.approvalId,
            comments = "V1 approved",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T11:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals("ver-01", approved.proofVersionId)
    }
}
