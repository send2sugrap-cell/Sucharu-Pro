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
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.design.ProofStatus
import com.sucharu.sucharupro.domain.model.design.RevisionReason
import com.sucharu.sucharupro.domain.model.design.RevisionRequestStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * End-to-End multi-cycle revision testing: V1 -> Rev1 -> V2 -> Rev2 -> V3 -> Rev3 -> V4 (Module 05 Step 03).
 */
class MultipleRevisionCycleTest {

    private lateinit var proofDataSource: FakeDesignProofDataSource
    private lateinit var proofRepository: DesignProofRepository

    private val fileV1 = FileReference("f1", "proof_v1.pdf", "application/pdf", 1000000L, "/files/v1.pdf", uploadedAt = "2026-08-16T10:00:00Z")
    private val fileV2 = FileReference("f2", "proof_v2.pdf", "application/pdf", 1100000L, "/files/v2.pdf", uploadedAt = "2026-08-16T11:00:00Z")
    private val fileV3 = FileReference("f3", "proof_v3.pdf", "application/pdf", 1200000L, "/files/v3.pdf", uploadedAt = "2026-08-16T12:00:00Z")
    private val fileV4 = FileReference("f4", "proof_v4.pdf", "application/pdf", 1300000L, "/files/v4.pdf", uploadedAt = "2026-08-16T13:00:00Z")

    @Before
    fun setUp() = runBlocking {
        val projDs = FakeDesignProjectDataSource(
            initialProjects = listOf(
                DesignProject(
                    projectId = "des-multi-01",
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
                    projectId = "des-multi-01",
                    productionJobId = "job-01",
                    name = "Artwork 1",
                    createdAt = "2026-08-16T10:00:00Z",
                    updatedAt = "2026-08-16T10:00:00Z"
                )
            )
        )
        val artRepo = DesignArtworkRepositoryImpl(artDs, projRepo)

        proofDataSource = FakeDesignProofDataSource()
        proofRepository = DesignProofRepositoryImpl(proofDataSource, artRepo)
    }

    @Test
    fun executeThreeFullRevisionCycles_maintainsCompleteTraceableHistory() = runBlocking {
        // 1. Initial Proof Creation (generates V1 in READY_FOR_REVIEW)
        val createRes = proofRepository.createProof(
            artworkId = "art-01",
            title = "বইয়ের কভার প্রুফ",
            initialArtworkVersionId = "art-v1",
            initialFile = fileV1,
            createdBy = "designer-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(createRes is DomainResult.Success)
        val proofId = (createRes as DomainResult.Success).data.proofId

        // CYCLE 1: V1 -> Rev1 -> V2
        val rev1Res = proofRepository.requestRevision(
            proofId = proofId,
            targetVersionNumber = 1,
            reason = RevisionReason.TEXT_CHANGE,
            notes = "Fix author subtitle font",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(rev1Res is DomainResult.Success)

        val startRev1 = proofRepository.startRevision(proofId = proofId, actorId = "designer-01", timestamp = "2026-08-16T10:45:00Z", callerRole = UserRole.DESIGNER)
        assertTrue(startRev1 is DomainResult.Success)

        val resubmit1 = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = "art-v2",
            fileReference = fileV2,
            notes = "Subtitle updated",
            createdBy = "designer-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(resubmit1 is DomainResult.Success)
        assertEquals(2, (resubmit1 as DomainResult.Success).data.versionNumber)

        // CYCLE 2: V2 -> Rev2 -> V3
        val rev2Res = proofRepository.requestRevision(
            proofId = proofId,
            targetVersionNumber = 2,
            reason = RevisionReason.COLOR_CHANGE,
            notes = "Adjust CMYK richness for spine",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T11:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(rev2Res is DomainResult.Success)

        val startRev2 = proofRepository.startRevision(proofId = proofId, actorId = "designer-01", timestamp = "2026-08-16T11:45:00Z", callerRole = UserRole.DESIGNER)
        assertTrue(startRev2 is DomainResult.Success)

        val resubmit2 = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = "art-v3",
            fileReference = fileV3,
            notes = "Spine color adjusted",
            createdBy = "designer-01",
            timestamp = "2026-08-16T12:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(resubmit2 is DomainResult.Success)
        assertEquals(3, (resubmit2 as DomainResult.Success).data.versionNumber)

        // CYCLE 3: V3 -> Rev3 -> V4
        val rev3Res = proofRepository.requestRevision(
            proofId = proofId,
            targetVersionNumber = 3,
            reason = RevisionReason.SPECIFICATION_CHANGE,
            notes = "Increase barcode padding to 5mm",
            requesterId = "mgr-01",
            timestamp = "2026-08-16T12:30:00Z",
            callerRole = UserRole.MANAGER
        )
        assertTrue(rev3Res is DomainResult.Success)

        val startRev3 = proofRepository.startRevision(proofId = proofId, actorId = "designer-01", timestamp = "2026-08-16T12:45:00Z", callerRole = UserRole.DESIGNER)
        assertTrue(startRev3 is DomainResult.Success)

        val resubmit3 = proofRepository.resubmitProof(
            proofId = proofId,
            artworkVersionId = "art-v4",
            fileReference = fileV4,
            notes = "Barcode padding increased to 5mm",
            createdBy = "designer-01",
            timestamp = "2026-08-16T13:00:00Z",
            callerRole = UserRole.DESIGNER
        )
        assertTrue(resubmit3 is DomainResult.Success)
        assertEquals(4, (resubmit3 as DomainResult.Success).data.versionNumber)

        // FINAL VERIFICATION
        val finalProof = (proofRepository.findProofById(proofId) as DomainResult.Success).data
        assertEquals(ProofStatus.RESUBMITTED, finalProof.status)
        assertEquals(4, finalProof.currentVersionNumber)
        assertEquals(4, finalProof.versions.size)
        assertEquals(3, finalProof.revisions.size)

        // All 3 revisions should be resolved
        assertTrue(finalProof.revisions.all { it.status == RevisionRequestStatus.RESOLVED })
        assertNull(finalProof.activeRevisionRequest)

        val versions = proofRepository.getProofVersions(proofId).first()
        assertEquals(listOf("V1", "V2", "V3", "V4"), versions.map { it.versionTag })
    }
}
