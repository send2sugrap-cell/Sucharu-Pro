package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for concrete QC Inspection Checklist instance creation (Module 06 Step 03).
 */
class QcInspectionChecklistTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleTemplate = QcChecklistTemplate(
        checklistTemplateId = "tmpl-insp-01",
        name = "কাটিং ও ফিনিশিং চেকলিস্ট",
        qcType = QcType.FINAL,
        version = 2,
        isActive = true,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        checklistDataSource = FakeQcChecklistDataSource(initialTemplates = listOf(sampleTemplate))
        checklistRepository = QcChecklistRepositoryImpl(checklistDataSource)
    }

    @Test
    fun createInspectionChecklist_capturesCurrentTemplateVersion() = runBlocking {
        val result = checklistRepository.createInspectionChecklist(
            inspectionId = "insp-01",
            templateId = "tmpl-insp-01",
            productionJobId = "job-01",
            productionQcId = "qc-01",
            productionStageId = "stage-cutting",
            notes = "কাটিং একুরেসি চেকিং",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val checklist = (result as DomainResult.Success).data
        assertNotNull(checklist.inspectionChecklistId)
        assertEquals(2, checklist.checklistTemplateVersion)
        assertEquals(QcChecklistStatus.READY, checklist.status)
        assertEquals("job-01", checklist.productionJobId)
    }
}
