package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProofDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
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
 * Tests for Revision Request Creation and Rules (Module 05 Step 03).
 */
class RevisionRequestTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

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

        proofDataSource = FakeDesignProofDataSource(
            initialProofs = listOf(sampleProof),
            initialVersions = listOf(versionV1)
        )
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artRepo)
    }

    @Test
    fun createRevisionRequest_transitionsProofToRevisionRequested() = runBlocking {
        val result = proofRepository.requestRevision(
            proofId = "prf-01",
            targetVersionNumber = 1,
            reason = RevisionReason.TEXT_CHANGE,
            notes = "টাইটেল ফন্ট সাইজ এবং স্পাইনের লেখক নাম সংশোধন করতে হবে।",
            requesterId = "mgr-01",
            requesterName = "ম্যানেজার সাহেব",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val rev = (result as DomainResult.Success).data
        assertNotNull(rev.requestId)
        assertEquals("prf-01", rev.proofId)
        assertEquals("prf-ver-1", rev.proofVersionId)
        assertEquals(1, rev.targetVersionNumber)
        assertEquals(RevisionReason.TEXT_CHANGE, rev.reason)
        assertEquals(RevisionRequestStatus.OPEN, rev.status)
        assertEquals("mgr-01", rev.requestedBy)

        val updatedProof = (proofRepository.findProofById("prf-01") as DomainResult.Success).data
        assertEquals(ProofStatus.REVISION_REQUESTED, updatedProof.status)
        assertEquals(1, updatedProof.revisions.size)
        assertNotNull(updatedProof.activeRevisionRequest)
    }

    @Test
    fun duplicateActiveRevisionRequest_fails() = runBlocking {
        // First request
        val res1 = proofRepository.requestRevision(
            proofId = "prf-01",
            targetVersionNumber = 1,
            reason = RevisionReason.LAYOUT_CHANGE,
            notes = "লেআউট পরিমার্জন",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(res1 is DomainResult.Success)

        // Second concurrent request on same proof while first is still open
        val res2 = proofRepository.requestRevision(
            proofId = "prf-01",
            targetVersionNumber = 1,
            reason = RevisionReason.COLOR_CHANGE,
            notes = "কালার চেঞ্জ",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T11:05:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(res2 is DomainResult.Error)
        val error = res2 as DomainResult.Error
        assertTrue(error.message.contains("Proof must be Ready for Review or Resubmitted") || error.message.contains("already open"))
    }
}
