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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency stress tests for Approval request and Final Lock operations (Module 05 Step 04).
 */
class ApprovalConcurrencyTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val sampleApproval = DesignApproval(
        approvalId = "appr-conc-01",
        projectId = "des-01",
        artworkId = "art-01",
        proofId = "prf-01",
        proofVersionId = "ver-01",
        artworkVersionId = "art-ver-01",
        targetProofVersionNumber = 1,
        status = ApprovalStatus.APPROVED,
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
    fun concurrentFinalLockAttempts_onlyOneSucceedsOrAllConsistentlyLock() = runBlocking {
        val deferreds = (1..5).map { i ->
            async {
                approvalRepository.lockFinalApproval(
                    approvalId = "appr-conc-01",
                    lockedBy = "user-$i",
                    timestamp = "2026-08-16T11:00:00Z",
                    callerRole = UserRole.MANAGER
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertTrue("At least one lock must succeed", successCount >= 1)

        val finalState = (approvalRepository.findApprovalById("appr-conc-01") as DomainResult.Success).data
        assertEquals(ApprovalStatus.FINAL_LOCKED, finalState.status)
        assertTrue(finalState.isFinalLocked)
    }
}
