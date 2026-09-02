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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Category G: Concurrency Stress Tests for Production Handoff (Module 05 Step 05).
 */
class ProductionHandoffConcurrencyTest {

    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkRepository: DesignArtworkRepository
    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalRepository: DesignApprovalRepository
    private lateinit var handoffRepository: DesignProductionHandoffRepository

    private val sampleFile = FileReference("f1", "cover.pdf", "application/pdf", 1000L, "/files/cover.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val sampleArtworkVersion = DesignArtworkVersion("art-ver-1", "art-01", 1, "V1", sampleFile, createdAt = "2026-08-16T10:00:00Z")
    private val sampleProofVersion = DesignProofVersion("prf-ver-1", "prf-01", 1, "V1", "art-ver-1", sampleFile, createdAt = "2026-08-16T10:00:00Z")

    private val finalLockedApproval = DesignApproval(
        approvalId = "appr-conc-01",
        projectId = "des-01",
        artworkId = "art-01",
        proofId = "prf-01",
        proofVersionId = "prf-ver-1",
        artworkVersionId = "art-ver-1",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.FINAL_LOCKED,
        isFinalLocked = true,
        finalApprovedProofVersionId = "prf-ver-1",
        finalApprovedArtworkVersionId = "art-ver-1",
        lockedAt = "2026-08-16T10:30:00Z",
        lockedBy = "mgr-01",
        requestedBy = "designer-01",
        requestedAt = "2026-08-16T10:00:00Z",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:30:00Z"
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
                    versions = listOf(sampleArtworkVersion),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            ),
            initialVersions = listOf(sampleArtworkVersion)
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
                    versions = listOf(sampleProofVersion),
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            ),
            initialVersions = listOf(sampleProofVersion)
        )
        proofRepository = DesignProofRepositoryImpl(proofDs, artworkRepository)

        val approvalDs = FakeDesignApprovalDataSource(initialApprovals = listOf(finalLockedApproval))
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
    fun concurrentHandoffRequests_onlyProduceOneUniqueRecord() = runBlocking {
        val deferreds = (1..5).map { i ->
            async {
                handoffRepository.authorizeProductionHandoff(
                    approvalId = "appr-conc-01",
                    authorizedBy = "user-$i",
                    timestamp = "2026-08-16T11:00:00Z",
                    callerRole = UserRole.MANAGER
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(5, successCount) // All return Success (idempotently resolving to the same authorized handoff)

        val allHandoffs = handoffRepository.observeHandoffs().first()
        assertEquals(1, allHandoffs.size)
    }
}
