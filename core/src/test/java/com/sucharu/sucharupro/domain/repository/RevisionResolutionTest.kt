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
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying that Revision resolution links to the resulting version and does NOT imply approval (Module 05 Step 03).
 */
class RevisionResolutionTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1200000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

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

        proofDataSource = FakeDesignProofDataSource()
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artRepo)
    }

    @Test
    fun resubmitProof_resolvesRevisionAndRecordsResolutionMetadata() = runBlocking {
        val createRes = proofRepository.createProof(
            artworkId = "art-01",
            title = "প্রধান প্রুফ",
            initialArtworkVersionId = "art-v1",
            initialFile = fileV1,
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        val proofId = (createRes as DomainResult.Success).data.proofId

        proofRepository.requestRevision(
            proofId = proofId,
            targetVersionNumber = 1,
            reason = RevisionReason.IMAGE_CHANGE,
            notes = "Replace cover illustration with high-res vector",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.MANAGER
        )

        proofRepository.startRevision(proofId = proofId, actorId = "designer-01", timestamp = "2026-08-16T10:45:00Z", callerRole = UserRole.DESIGNER)

        val resubmitRes = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = "art-v2",
            fileReference = fileV2,
            notes = "High-res vector inserted",
            createdBy = "designer-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(resubmitRes is DomainResult.Success)
        val versionV2 = (resubmitRes as DomainResult.Success).data

        val proof = (proofRepository.findProofById(proofId) as DomainResult.Success).data
        val resolvedRevision = proof.revisions.first()

        assertEquals(RevisionRequestStatus.RESOLVED, resolvedRevision.status)
        assertEquals("designer-01", resolvedRevision.resolvedBy)
        assertEquals("2026-08-16T11:00:00Z", resolvedRevision.resolvedAt)
        assertEquals(versionV2.versionId, resolvedRevision.resultingProofVersionId)
        assertEquals(2, resolvedRevision.resultingVersionNumber)
        assertEquals(ProofStatus.RESUBMITTED, proof.status)
    }
}
