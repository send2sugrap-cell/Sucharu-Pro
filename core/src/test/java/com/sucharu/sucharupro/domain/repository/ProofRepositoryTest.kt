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
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CRUD and observation in [DesignProofRepository] (Module 05 Step 03).
 */
class ProofRepositoryTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val sampleProof = DesignProof(
        proofId = "prf-crud-01",
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        title = "প্রধান প্রুফ",
        status = ProofStatus.DRAFT,
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

        proofDataSource = FakeDesignProofDataSource(initialProofs = listOf(sampleProof))
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artRepo)
    }

    @Test
    fun getProofById_existing_returnsProof() = runBlocking {
        val proof = proofRepository.getProofById("prf-crud-01").first()
        assertNotNull(proof)
        assertEquals("প্রধান প্রুফ", proof?.title)
    }

    @Test
    fun getProofById_nonExistent_returnsNull() = runBlocking {
        val proof = proofRepository.getProofById("non-existent").first()
        assertNull(proof)
    }

    @Test
    fun submitProofForReview_transitionsDraftToReadyForReview() = runBlocking {
        val result = proofRepository.submitProofForReview(
            proofId = "prf-crud-01",
            actorId = "designer-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals(ProofStatus.READY_FOR_REVIEW, updated.status)
    }

    @Test
    fun archiveProof_transitionsToArchived() = runBlocking {
        val result = proofRepository.archiveProof(
            proofId = "prf-crud-01",
            archivedBy = "Manager",
            reason = "Superseded",
            timestamp = "2026-08-16T12:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val archived = (result as DomainResult.Success).data
        assertEquals(ProofStatus.ARCHIVED, archived.status)
        assertTrue(archived.isArchived)
    }
}
