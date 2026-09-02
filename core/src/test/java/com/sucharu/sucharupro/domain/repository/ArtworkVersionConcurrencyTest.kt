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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency and race-condition safety tests for Artwork Versioning (Module 05 Step 02).
 */
class ArtworkVersionConcurrencyTest {

    private lateinit var projectDataSource: FakeDesignProjectDataSource
    private lateinit var projectRepository: DesignProjectRepository
    private lateinit var artworkDataSource: FakeDesignArtworkDataSource
    private lateinit var artworkRepository: DesignArtworkRepository

    private val sampleProject = DesignProject(
        projectId = "des-conc-01",
        projectNumber = "DES-2026-0001",
        productionJobId = "job-conc-01",
        orderId = "ord-conc-01",
        orderNumber = "ORD-CONC-0001",
        customerId = "cus-conc-01",
        title = "বই কভার ডিজাইন",
        status = DesignStatus.IN_DESIGN,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleArtwork = DesignArtwork(
        artworkId = "art-conc-01",
        projectId = "des-conc-01",
        productionJobId = "job-conc-01",
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
    fun concurrentVersionUploads_produceDeterministicSequentialVersions() = runBlocking {
        val count = 10
        val deferreds = (1..count).map { i ->
            async {
                val file = FileReference(
                    fileId = "file-$i",
                    fileName = "cover_v$i.pdf",
                    mimeType = "application/pdf",
                    fileSize = 1000000L + (i * 10000),
                    storagePath = "/storage/v$i.pdf",
                    uploadedAt = "2026-08-16T10:00:00Z"
                )
                artworkRepository.createArtworkVersion(
                    artworkId = "art-conc-01",
                    fileReference = file,
                    notes = "Concurrent upload $i",
                    timestamp = "2026-08-16T10:00:00Z",
                    callerRole = UserRole.DESIGNER
                )
            }
        }

        val results = deferreds.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val versions = artworkRepository.getArtworkVersions("art-conc-01").first()
        assertEquals(count, versions.size)

        val versionNumbers = versions.map { it.versionNumber }
        assertEquals((1..count).toList(), versionNumbers)

        val updatedArtwork = (artworkRepository.findArtworkById("art-conc-01") as DomainResult.Success).data
        assertEquals(count, updatedArtwork.currentVersionNumber)
    }
}
