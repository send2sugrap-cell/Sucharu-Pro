package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProofDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProofRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkStatus
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.design.DesignProject
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
 * Tests for Proof Creation & Initial State (Module 05 Step 03).
 */
class ProofCreationTest {

    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkRepository: DesignArtworkRepository
    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val sampleProject = DesignProject(
        projectId = "des-prf-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.IN_DESIGN,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleArtwork = DesignArtwork(
        artworkId = "art-01",
        projectId = "des-prf-01",
        productionJobId = "job-01",
        name = "প্রধান প্রচ্ছদ",
        currentVersionNumber = 1,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleFile = FileReference(
        fileId = "f1",
        fileName = "proof_v1.pdf",
        mimeType = "application/pdf",
        fileSize = 2500000L,
        storagePath = "/files/proof_v1.pdf",
        uploadedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        val projDs = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        projectRepository = DesignProjectRepositoryImpl(projDs)

        val artDs = FakeDesignArtworkDataSource(initialArtworks = listOf(sampleArtwork))
        artworkRepository = DesignArtworkRepositoryImpl(artDs, projectRepository)

        proofDataSource = FakeDesignProofDataSource()
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artworkRepository)
    }

    @Test
    fun createProof_withInitialFile_createsProofAndV1InReadyForReview() = runBlocking {
        val result = proofRepository.createProof(
            artworkId = "art-01",
            title = "প্রথম প্রুফ কপি",
            initialArtworkVersionId = "art-ver-1",
            initialFile = sampleFile,
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val proof = (result as DomainResult.Success).data
        assertNotNull(proof.proofId)
        assertEquals("প্রথম প্রুফ কপি", proof.title)
        assertEquals("art-01", proof.artworkId)
        assertEquals("des-prf-01", proof.projectId)
        assertEquals("job-01", proof.productionJobId)
        assertEquals(ProofStatus.READY_FOR_REVIEW, proof.status)
        assertEquals(1, proof.currentVersionNumber)
        assertEquals(1, proof.versions.size)
        assertEquals("V1", proof.versions.first().versionTag)

        val persisted = proofRepository.observeProofs().first()
        assertEquals(1, persisted.size)
    }

    @Test
    fun createProof_withoutInitialFile_createsProofInDraft() = runBlocking {
        val result = proofRepository.createProof(
            artworkId = "art-01",
            title = "খসড়া প্রুফ",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val proof = (result as DomainResult.Success).data
        assertEquals(ProofStatus.DRAFT, proof.status)
        assertEquals(0, proof.currentVersionNumber)
        assertEquals(0, proof.versions.size)
    }

    @Test
    fun createProof_invalidArtwork_fails() = runBlocking {
        val result = proofRepository.createProof(
            artworkId = "non-existent-art",
            title = "প্রুফ",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Artwork 'non-existent-art' not found"))
    }
}
