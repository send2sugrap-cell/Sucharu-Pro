package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for attaching supporting files and evidence to a Rework record (Module 06 Step 05).
 */
class ProductionReworkEvidenceTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun attachEvidence_withValidFileReference_succeeds() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.SPOT_UV_CORRECTION,
            reason = ReworkReason.FINISHING_ERROR,
            affectedQuantity = 80,
            quantityUnit = "sheets",
            description = "Spot UV registration misaligned by 2mm",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId

        val fileRef = FileReference(
            fileId = "file-uv-01",
            fileName = "spot_uv_defect_photo.jpg",
            mimeType = "image/jpeg",
            storagePath = "/uploads/qc/spot_uv_defect_photo.jpg",
            fileSize = 1048576L,
            uploadedAt = "2026-08-17T10:00:00Z"
        )

        val attachRes = repository.attachEvidence(
            reworkId = reworkId,
            fileReference = fileRef,
            description = "Macro photograph of Spot UV alignment plate offset",
            attachedBy = "insp-01",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(attachRes is DomainResult.Success)
        val evidenceList = repository.observeEvidence(reworkId).first()
        assertEquals(1, evidenceList.size)
        assertEquals("Macro photograph of Spot UV alignment plate offset", evidenceList[0].description)
        assertEquals("file-uv-01", evidenceList[0].fileReferenceId)
    }
}
