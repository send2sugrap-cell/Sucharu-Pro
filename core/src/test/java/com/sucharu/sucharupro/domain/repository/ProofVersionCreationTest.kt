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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for deterministic Proof Version creation and sequencing (Module 05 Step 03).
 */
class ProofVersionCreationTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val sampleProof = DesignProof(
        proofId = "prf-01",
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        title = "প্রধান প্রচ্ছদ প্রুফ",
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
    fun createSequentialVersions_generatesDeterministicVersionNumbers() = runBlocking {
        val file1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
        val file2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1100000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

        val v1Res = proofRepository.createProofVersion(
            proofId = "prf-01",
            artworkVersionId = "art-ver-1",
            fileReference = file1,
            notes = "Initial proof",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v1Res is DomainResult.Success)
        val v1 = (v1Res as DomainResult.Success).data
        assertEquals(1, v1.versionNumber)
        assertEquals("V1", v1.versionTag)

        val v2Res = proofRepository.createProofVersion(
            proofId = "prf-01",
            artworkVersionId = "art-ver-2",
            fileReference = file2,
            notes = "Revised colors",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v2Res is DomainResult.Success)
        val v2 = (v2Res as DomainResult.Success).data
        assertEquals(2, v2.versionNumber)
        assertEquals("V2", v2.versionTag)

        val allVersions = proofRepository.getProofVersions("prf-01").first()
        assertEquals(2, allVersions.size)
        assertEquals(listOf("V1", "V2"), allVersions.map { it.versionTag })
    }
}
