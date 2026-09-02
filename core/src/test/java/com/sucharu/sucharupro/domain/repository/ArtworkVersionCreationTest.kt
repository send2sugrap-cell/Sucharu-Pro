package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDesignArtworkDataSource
import com.sucharu.sucharupro.data.datasource.FakeDesignProjectDataSource
import com.sucharu.sucharupro.data.repository.DesignArtworkRepositoryImpl
import com.sucharu.sucharupro.data.repository.DesignProjectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.design.ArtworkMetadata
import com.sucharu.sucharupro.domain.model.design.DesignArtwork
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for deterministic Artwork Version creation and progression (V1 -> V2 -> V3) (Module 05 Step 02).
 */
class ArtworkVersionCreationTest {

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

    private val sampleArtwork = DesignArtwork(
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        name = "প্রধান প্রচ্ছদ",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() = runBlocking {
        projectDataSource = FakeDesignProjectDataSource(initialProjects = listOf(sampleProject))
        projectRepository = DesignProjectRepositoryImpl(projectDataSource)
        artworkDataSource = FakeDesignArtworkDataSource(initialArtworks = listOf(sampleArtwork))
        artworkRepository = DesignArtworkRepositoryImpl(artworkDataSource, projectRepository)
    }

    @Test
    fun createSequentialVersions_generatesDeterministicVersionNumbers() = runBlocking {
        val file1 = FileReference("f1", "cover_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
        val file2 = FileReference("f2", "cover_v2.ai", "application/illustrator", 2500000L, "/files/v2.ai", uploadedAt = "2026-08-16T11:00:00Z")
        val file3 = FileReference("f3", "cover_v3.pdf", "application/pdf", 3200000L, "/files/v3.pdf", uploadedAt = "2026-08-16T12:00:00Z")

        // Create V1
        val v1Res = artworkRepository.createArtworkVersion(
            artworkId = "art-01",
            fileReference = file1,
            notes = "Initial draft",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v1Res is DomainResult.Success)
        val v1 = (v1Res as DomainResult.Success).data
        assertEquals(1, v1.versionNumber)
        assertEquals("V1", v1.versionTag)

        // Create V2
        val v2Res = artworkRepository.createArtworkVersion(
            artworkId = "art-01",
            fileReference = file2,
            notes = "Spine width adjusted",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v2Res is DomainResult.Success)
        val v2 = (v2Res as DomainResult.Success).data
        assertEquals(2, v2.versionNumber)
        assertEquals("V2", v2.versionTag)

        // Create V3
        val v3Res = artworkRepository.createArtworkVersion(
            artworkId = "art-01",
            fileReference = file3,
            notes = "High-res PDF with 3mm bleed",
            timestamp = "2026-08-16T12:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(v3Res is DomainResult.Success)
        val v3 = (v3Res as DomainResult.Success).data
        assertEquals(3, v3.versionNumber)
        assertEquals("V3", v3.versionTag)

        // Verify parent artwork sync
        val artwork = artworkRepository.findArtworkById("art-01")
        assertTrue(artwork is DomainResult.Success)
        val data = (artwork as DomainResult.Success).data
        assertEquals(3, data.currentVersionNumber)
        assertEquals(3, data.versions.size)
        assertEquals("V3", data.currentVersion?.versionTag)

        val versions = artworkRepository.getArtworkVersions("art-01").first()
        assertEquals(3, versions.size)
        assertEquals(listOf("V1", "V2", "V3"), versions.map { it.versionTag })
    }
}
