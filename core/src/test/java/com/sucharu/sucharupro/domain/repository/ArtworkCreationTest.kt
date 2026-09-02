package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.ArtworkStatus
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
 * Tests for Artwork Creation & Validation (Module 05 Step 02).
 */
class ArtworkCreationTest {

    private lateinit var projectDataSource: FakeDesignProjectDataSource
    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkDataSource: FakeDesignArtworkDataSource
    private lateinit var artworkRepository: DesignArtworkRepository

    private val sampleProject = DesignProject(
        projectId = "des-art-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-01",
        orderId = "ord-01",
        orderNumber = "ORD-2026-0001",
        customerId = "cus-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.IN_DESIGN,
        assignedDesignerId = "designer-01",
        assignedDesignerName = "তানভীর হাসান",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleFile = FileReference(
        fileId = "file-01",
        fileName = "cover_final_v1.pdf",
        mimeType = "application/pdf",
        fileSize = 4500000L,
        storagePath = "/storage/artworks/des-art-01/cover_final_v1.pdf",
        checksum = "e3b0c44298fc1c149afbf4c8996fb924",
        uploadedAt = "2026-08-16T10:30:00Z",
        uploadedBy = "designer-01"
    )

    @Before
    fun setUp() = runBlocking {
        projectDataSource = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        projectRepository = DesignProjectRepositoryImpl(projectDataSource)
        artworkDataSource = FakeDesignArtworkDataSource()
        artworkRepository = DesignArtworkRepositoryImpl(artworkDataSource, projectRepository)
    }

    @Test
    fun createArtwork_withInitialFile_createsArtworkAndV1Successfully() = runBlocking {
        val result = artworkRepository.createArtwork(
            projectId = "des-art-01",
            name = "বইয়ের প্রধান প্রচ্ছদ",
            description = "চার কালার ফ্রন্ট ও ব্যাক প্রচ্ছদ ডিজাইন",
            initialFile = sampleFile,
            initialMetadata = ArtworkMetadata(width = 8.5, height = 11.0, unit = "in", colorMode = "CMYK"),
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue("Expected Success result", result is DomainResult.Success)
        val artwork = (result as DomainResult.Success).data
        assertNotNull(artwork.artworkId)
        assertEquals("বইয়ের প্রধান প্রচ্ছদ", artwork.name)
        assertEquals("des-art-01", artwork.projectId)
        assertEquals("job-01", artwork.productionJobId)
        assertEquals(1, artwork.currentVersionNumber)
        assertEquals(ArtworkStatus.ACTIVE, artwork.status)
        assertEquals(1, artwork.versions.size)
        assertEquals("V1", artwork.versions.first().versionTag)
        assertEquals("cover_final_v1.pdf", artwork.versions.first().fileReference.fileName)

        val persisted = artworkRepository.observeArtworks().first()
        assertEquals(1, persisted.size)
    }

    @Test
    fun createArtwork_withoutInitialFile_createsEmptyArtwork() = runBlocking {
        val result = artworkRepository.createArtwork(
            projectId = "des-art-01",
            name = "ইনার পেজ লেআউট",
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val artwork = (result as DomainResult.Success).data
        assertEquals(0, artwork.currentVersionNumber)
        assertEquals(0, artwork.versions.size)
    }

    @Test
    fun createArtwork_invalidProject_fails() = runBlocking {
        val result = artworkRepository.createArtwork(
            projectId = "invalid-project",
            name = "বই কভার",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Design Project 'invalid-project' not found"))
    }

    @Test
    fun createArtwork_duplicateNameInSameProject_fails() = runBlocking {
        val res1 = artworkRepository.createArtwork(
            projectId = "des-art-01",
            name = "প্রধান প্রচ্ছদ",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(res1 is DomainResult.Success)

        val res2 = artworkRepository.createArtwork(
            projectId = "des-art-01",
            name = "প্রধান প্রচ্ছদ",
            timestamp = "2026-08-16T10:40:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(res2 is DomainResult.Error)
        val error = res2 as DomainResult.Error
        assertTrue(error.message.contains("already exists in Design Project"))
    }
}
