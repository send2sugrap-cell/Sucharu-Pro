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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying that FINAL_LOCKED status is strictly immutable and cannot be unlocked or altered (Module 05 Step 04).
 */
class FinalApprovalLockTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val sampleApproval = DesignApproval(
        approvalId = "appr-lock-01",
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
    fun lockFinalApproval_locksSuccessfullyAndBlocksFurtherDecisions() = runBlocking {
        val lockRes = approvalRepository.lockFinalApproval(
            approvalId = "appr-lock-01",
            lockedBy = "admin-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(lockRes is DomainResult.Success)
        val locked = (lockRes as DomainResult.Success).data
        assertEquals(ApprovalStatus.FINAL_LOCKED, locked.status)
        assertTrue(locked.isFinalLocked)
        assertEquals("ver-01", locked.finalApprovedProofVersionId)
        assertEquals("art-ver-01", locked.finalApprovedArtworkVersionId)

        // Attempting another decision after final lock must fail
        val rejectAttempt = approvalRepository.reject(
            approvalId = "appr-lock-01",
            comments = "Late reject",
            reviewerId = "admin-01",
            timestamp = "2026-08-16T11:30:00Z",
            callerRole = UserRole.ADMIN
        )
        assertTrue(rejectAttempt is DomainResult.Error)
        val error = rejectAttempt as DomainResult.Error
        assertTrue(error.message.contains("already Final Locked"))
    }
}
