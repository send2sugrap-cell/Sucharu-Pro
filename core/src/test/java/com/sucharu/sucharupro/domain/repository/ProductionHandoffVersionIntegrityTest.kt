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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Category B: Version Integrity Tests (Module 05 Step 05).
 */
class ProductionHandoffVersionIntegrityTest {

    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkRepository: DesignArtworkRepository
    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalRepository: DesignApprovalRepository
    private lateinit var handoffRepository: DesignProductionHandoffRepository

    private val fileV1 = FileReference("f1", "cover_v1.pdf", "application/pdf", 1000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "cover_v2.pdf", "application/pdf", 1100L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

    private val artV1 = DesignArtworkVersion("art-v1", "art-01", 1, "V1", fileV1, createdAt = "2026-08-16T10:00:00Z")
    private val artV2 = DesignArtworkVersion("art-v2", "art-01", 2, "V2", fileV2, createdAt = "2026-08-16T11:00:00Z")

    private val proofV1 = DesignProofVersion("prf-v1", "prf-01", 1, "V1", "art-v1", fileV1, status = ProofStatus.RESUBMITTED, createdAt = "2026-08-16T10:00:00Z")
    private val proofV2 = DesignProofVersion("prf-v2", "prf-01", 2, "V2", "art-v2", fileV2, status = ProofStatus.READY_FOR_REVIEW, createdAt = "2026-08-16T11:00:00Z")

    private val finalApprovalV2 = DesignApproval(
        approvalId = "appr-v2",
        projectId = "des-01",
        artworkId = "art-01",
        proofId = "prf-01",
        proofVersionId = "prf-v2",
        artworkVersionId = "art-v2",
        targetProofVersionNumber = 2,
        status = ApprovalStatus.FINAL_LOCKED,
        isFinalLocked = true,
        finalApprovedProofVersionId = "prf-v2",
        finalApprovedArtworkVersionId = "art-v2",
        lockedAt = "2026-08-16T11:30:00Z",
        lockedBy = "mgr-01",
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T11:15:00Z",
        createdAt = "2026-08-16T11:15:00Z",
        updatedAt = "2026-08-16T11:30:00Z"
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
        projectRepository = DesignProjectRepositoryImpl(projDs)

        val artDs = FakeDesignArtworkDataSource(
            initialArtworks = listOf(
                DesignArtwork(
                    artworkId = "art-01",
                    projectId = "des-01",
                    productionJobId = "job-01",
                    name = "Artwork 1",
                    currentVersionNumber = 2,
                    versions = listOf(artV1, artV2),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T11:00:00Z"
                )
            ),
            initialVersions = listOf(artV1, artV2)
        )
        artworkRepository = DesignArtworkRepositoryImpl(artDs, projectRepository)

        val proofDs = FakeDesignProofDataSource(
            initialProofs = listOf(
                DesignProof(
                    proofId = "prf-01",
                    artworkId = "art-01",
                    projectId = "des-01",
                    productionJobId = "job-01",
                    title = "Proof",
                    status = ProofStatus.READY_FOR_REVIEW,
                    currentVersionNumber = 2,
                    versions = listOf(proofV1, proofV2),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T11:00:00Z"
                )
            ),
            initialVersions = listOf(proofV1, proofV2)
        )
        proofRepository = DesignProofRepositoryImpl(proofDs, artworkRepository)

        val approvalDs = FakeDesignApprovalDataSource(initialApprovals = listOf(finalApprovalV2))
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
    fun exactApprovedVersionsHandedOff_preservesIntegrity() = runBlocking {
        val handoffRes = handoffRepository.authorizeProductionHandoff(
            approvalId = "appr-v2",
            authorizedBy = "mgr-01",
            timestamp = "2026-08-16T11:45:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data
        assertEquals("prf-v2", handoff.proofVersionId)
        assertEquals("art-v2", handoff.artworkVersionId)
    }
}
