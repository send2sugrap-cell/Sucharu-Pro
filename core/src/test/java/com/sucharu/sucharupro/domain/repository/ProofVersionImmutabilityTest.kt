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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying that historical Proof versions are strictly immutable (Module 05 Step 03).
 */
class ProofVersionImmutabilityTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1200000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

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
    fun creatingNewVersion_preservesOriginalV1Intact() = runBlocking {
        val v2Res = proofRepository.createProofVersion(
            proofId = "prf-01",
            artworkVersionId = "art-ver-2",
            fileReference = fileV2,
            notes = "Revised edition",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v2Res is DomainResult.Success)

        // Verify V1 is intact
        val v1Retrieved = proofRepository.getProofVersion("prf-01", 1).first()
        assertNotNull(v1Retrieved)
        assertEquals(1, v1Retrieved?.versionNumber)
        assertEquals("proof_v1.pdf", v1Retrieved?.fileReference?.fileName)
        assertEquals("Original V1", v1Retrieved?.notes)

        // Verify V2 exists
        val v2Retrieved = proofRepository.getProofVersion("prf-01", 2).first()
        assertNotNull(v2Retrieved)
        assertEquals(2, v2Retrieved?.versionNumber)
        assertEquals("proof_v2.pdf", v2Retrieved?.fileReference?.fileName)
        assertEquals("Revised edition", v2Retrieved?.notes)
    }
}
