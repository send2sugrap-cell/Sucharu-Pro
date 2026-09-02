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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CRUD and observation operations in [DesignArtworkRepository] (Module 05 Step 02).
 */
class ArtworkRepositoryTest {

    private lateinit var projectDataSource: FakeDesignProjectDataSource
    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkDataSource: FakeDesignArtworkDataSource
    private lateinit var artworkRepository: DesignArtworkRepository

    private val sampleProject = DesignProject(
        projectId = "des-repo-01",
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

    private val sampleFile = FileReference("f1", "cover.pdf", "application/pdf", 1000000L, "/files/cover.pdf", uploadedAt = "2026-08-16T10:00:00Z")

    private val artwork1 = DesignArtwork(
        artworkId = "art-01",
        projectId = "des-repo-01",
        productionJobId = "job-01",
        name = "প্রধান প্রচ্ছদ",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        projectDataSource = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        projectRepository = DesignProjectRepositoryImpl(projectDataSource)
        artworkDataSource = FakeDesignArtworkDataSource(initialArtworks = listOf(artwork1))
        artworkRepository = DesignArtworkRepositoryImpl(artworkDataSource, projectRepository)
    }

    @Test
    fun getArtworkById_existingArtwork_returnsArtwork() = runBlocking {
        val found = artworkRepository.getArtworkById("art-01").first()
        assertNotNull(found)
        assertEquals("প্রধান প্রচ্ছদ", found?.name)
    }

    @Test
    fun getArtworkById_nonExistent_returnsNull() = runBlocking {
        val found = artworkRepository.getArtworkById("non-existent").first()
        assertNull(found)
    }

    @Test
    fun updateArtworkMetadata_updatesNameAndDescription() = runBlocking {
        val result = artworkRepository.updateArtworkMetadata(
            artworkId = "art-01",
            name = "আপডেটেড প্রচ্ছদ নাম",
            description = "নতুন ডেসক্রিপশন",
            updatedBy = "designer-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )

        assertTrue(result is DomainResult.Success)
        val updated = (result as DomainResult.Success).data
        assertEquals("আপডেটেড প্রচ্ছদ নাম", updated.name)
        assertEquals("নতুন ডেসক্রিপশন", updated.description)
    }

    @Test
    fun archiveArtwork_setsArchivedStatus() = runBlocking {
        val result = artworkRepository.archiveArtwork(
            artworkId = "art-01",
            archivedBy = "Manager",
            reason = "Superseded",
            timestamp = "2026-08-16T12:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(result is DomainResult.Success)
        val archived = (result as DomainResult.Success).data
        assertEquals(ArtworkStatus.ARCHIVED, archived.status)
        assertTrue(archived.isArchived)
    }

    @Test
    fun archiveArtworkVersion_setsVersionArchivedStatus() = runBlocking {
        val v1Res = artworkRepository.createArtworkVersion(
            artworkId = "art-01",
            fileReference = sampleFile,
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v1Res is DomainResult.Success)

        val archiveRes = artworkRepository.archiveArtworkVersion(
            artworkId = "art-01",
            versionNumber = 1,
            archivedBy = "Manager",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(archiveRes is DomainResult.Success)
        val v1Archived = (archiveRes as DomainResult.Success).data
        assertEquals(ArtworkStatus.ARCHIVED, v1Archived.status)
    }
}
