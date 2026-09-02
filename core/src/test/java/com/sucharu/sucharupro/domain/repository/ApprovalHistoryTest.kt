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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying immutable historical approval decisions (Module 05 Step 04).
 */
class ApprovalHistoryTest {

    private lateinit var proofRepository: DesignProofRepository
    private lateinit var approvalDataSource: FakeDesignApprovalDataSource
    private lateinit var approvalRepository: DesignApprovalRepository

    private val sampleApproval = DesignApproval(
        approvalId = "appr-hist-01",
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
    fun approvalDecisions_areRecordedInChronologicalHistory() = runBlocking {
        val result = approvalRepository.requestRevision(
            approvalId = "appr-hist-01",
            reason = RevisionReason.TEXT_CHANGE,
            comments = "কপিরাইটিং সংশোধন প্রয়োজন।",
            reviewerId = "mgr-01",
            reviewerName = "ম্যানেজার",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(result is DomainResult.Success)

        val history = approvalRepository.getApprovalHistory("appr-hist-01").first()
        assertEquals(1, history.size)
        assertEquals(ApprovalDecisionType.REVISION_REQUIRED, history.first().decisionType)
        assertEquals("কপিরাইটিং সংশোধন প্রয়োজন।", history.first().comments)
    }
}
