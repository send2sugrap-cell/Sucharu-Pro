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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency stress tests for Proof Version sequencing (Module 05 Step 03).
 */
class ProofVersionConcurrencyTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val sampleProof = DesignProof(
        proofId = "prf-conc-01",
        artworkId = "art-01",
        projectId = "des-01",
        productionJobId = "job-01",
        title = "কনকারেন্সি টেস্ট প্রুফ",
        status = ProofStatus.READY_FOR_REVIEW,
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
    fun concurrentProofVersionUploads_produceDeterministicSequentialVersions() = runBlocking {
        val count = 10
        val deferreds = (1..count).map { i ->
            async {
                val file = FileReference(
                    fileId = "file-$i",
                    fileName = "proof_v$i.pdf",
                    mimeType = "application/pdf",
                    fileSize = 1000000L + (i * 10000),
                    storagePath = "/storage/v$i.pdf",
                    uploadedAt = "2026-08-16T10:00:00Z"
                )
                proofRepository.createProofVersion(
                    proofId = "prf-conc-01",
                    artworkVersionId = "art-ver-$i",
                    fileReference = file,
                    notes = "Concurrent upload $i",
                    timestamp = "2026-08-16T10:00:00Z",
                    callerRole = UserRole.DESIGNER
                )
            }
        }

        val results = deferreds.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val versions = proofRepository.getProofVersions("prf-conc-01").first()
        assertEquals(count, versions.size)

        val versionNumbers = versions.map { it.versionNumber }
        assertEquals((1..count).toList(), versionNumbers)

        val updatedProof = (proofRepository.findProofById("prf-conc-01") as DomainResult.Success).data
        assertEquals(count, updatedProof.currentVersionNumber)
    }
}
