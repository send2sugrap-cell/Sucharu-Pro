package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.ArtworkStatus
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignArtworkVersion
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying that historical Artwork versions are immutable and cannot be destructively overwritten (Module 05 Step 02).
 */
class ArtworkVersionImmutabilityTest {

    private lateinit var projectDataSource: FakeDesignProjectDataSource
    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkDataSource: FakeDesignArtworkDataSource
    private lateinit var artworkRepository: DesignArtworkRepository

    private val sampleProject = DesignProject(
        projectId = "des-01",
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

    private val fileV1 = FileReference("f1", "cover_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "cover_v2.pdf", "application/pdf", 1200000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")

    private val versionV1 = DesignArtworkVersion(
        versionId = "ver-1",
        artworkId = "art-01",
        versionNumber = 1,
        versionTag = "V1",
        fileReference = fileV1,
        metadata = ArtworkMetadata(width = 8.5, height = 11.0, colorMode = "CMYK"),
        status = ArtworkStatus.ACTIVE,
        notes = "Original V1 draft",
        createdAt = "2026-08-16T10:00:00Z",
        createdBy = "designer-01"
    )

    private val sampleArtwork = DesignArtwork(
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        name = "প্রধান প্রচ্ছদ",
        currentVersionNumber = 1,
        versions = listOf(versionV1),
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        projectDataSource = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        projectRepository = DesignProjectRepositoryImpl(projectDataSource)
        artworkDataSource = FakeDesignArtworkDataSource(
            initialArtworks = listOf(sampleArtwork),
            initialVersions = listOf(versionV1)
        )
        artworkRepository = DesignArtworkRepositoryImpl(artworkDataSource, projectRepository)
    }

    @Test
    fun creatingNewVersion_preservesOriginalV1Intact() = runBlocking {
        val v2Res = artworkRepository.createArtworkVersion(
            artworkId = "art-01",
            fileReference = fileV2,
            metadata = ArtworkMetadata(width = 8.5, height = 11.0, colorMode = "CMYK", bleedMarginMm = 3.0),
            notes = "Bleed margin added",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v2Res is DomainResult.Success)

        // Retrieve historical V1
        val v1Retrieved = artworkRepository.getArtworkVersion("art-01", 1).first()
        assertNotNull(v1Retrieved)
        assertEquals(1, v1Retrieved?.versionNumber)
        assertEquals("cover_v1.pdf", v1Retrieved?.fileReference?.fileName)
        assertEquals("Original V1 draft", v1Retrieved?.notes)
        assertEquals(1000000L, v1Retrieved?.fileReference?.fileSize)

        // Retrieve V2
        val v2Retrieved = artworkRepository.getArtworkVersion("art-01", 2).first()
        assertNotNull(v2Retrieved)
        assertEquals(2, v2Retrieved?.versionNumber)
        assertEquals("cover_v2.pdf", v2Retrieved?.fileReference?.fileName)
        assertEquals("Bleed margin added", v2Retrieved?.notes)

        val allVersions = artworkRepository.getArtworkVersions("art-01").first()
        assertEquals(2, allVersions.size)
    }
}
