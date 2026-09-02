package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [DefectEvidence] attachment and reference handling (Module 06 Step 04).
 */
class ProductionDefectEvidenceTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val sampleDefect = ProductionDefect(
        defectId = "def-evi-01",
        productionJobId = "job-01",
        category = DefectCategory.PRINT_QUALITY,
        severity = DefectSeverity.MAJOR,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.OPEN,
        title = "Streak lines on black channel",
        description = "Horizontal streak lines appearing across pages 4 and 5.",
        affectedQuantity = 120,
        detectedAt = "2026-08-17T10:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private val sampleFileRef = FileReference(
        fileId = "file-photo-01",
        fileName = "streak_defect.jpg",
        mimeType = "image/jpeg",
        fileSize = 1024 * 500,
        storagePath = "/uploads/qc/streak_defect.jpg",
        uploadedAt = "2026-08-17T10:05:00Z",
        uploadedBy = "insp-01"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(sampleDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun attachEvidence_validFileReference_attachesToDefectAndRecordsActivity() = runBlocking {
        val result = repository.attachEvidence(
            defectId = "def-evi-01",
            fileReferenceId = "file-photo-01",
            fileReference = sampleFileRef,
            description = "High resolution photo of cylinder streak lines",
            attachedBy = "insp-01",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val evidence = (result as DomainResult.Success).data
        assertNotNull(evidence.evidenceId)
        assertEquals("def-evi-01", evidence.defectId)
        assertEquals("file-photo-01", evidence.fileReferenceId)

        val defect = repository.observeDefectById("def-evi-01").first()!!
        assertEquals(1, defect.evidenceList.size)
        assertEquals("file-photo-01", defect.evidenceList[0].fileReferenceId)

        val activities = repository.observeDefectActivity("def-evi-01").first()
        assertTrue(activities.any { it.activityType.name == "DEFECT_EVIDENCE_ATTACHED" })
    }
}
