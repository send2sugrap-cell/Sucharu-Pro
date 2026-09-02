package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for linking [ProductionDefect] with QC inspections and checklists (Module 06 Step 04).
 */
class ProductionDefectQcIntegrationTest {

    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var defectRepository: ProductionDefectRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-linked-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.FAILED,
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private val sampleChecklist = QcInspectionChecklist(
        inspectionChecklistId = "chk-linked-01",
        inspectionId = "insp-session-01",
        checklistTemplateId = "tmpl-01",
        checklistTemplateVersion = 1,
        productionJobId = "job-01",
        productionQcId = "qc-linked-01",
        status = QcChecklistStatus.IN_PROGRESS,
        createdAt = "2026-08-17T10:00:00Z"
    )

    @Before
    fun setUp() {
        defectDataSource = FakeProductionDefectDataSource()
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
        checklistDataSource = FakeQcChecklistDataSource(initialChecklists = listOf(sampleChecklist))
        defectRepository = ProductionDefectRepositoryImpl(
            defectDataSource = defectDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource
        )
    }

    @Test
    fun createDefect_fromFailedQcAndChecklist_preservesTraceability() = runBlocking {
        val result = defectRepository.createDefect(
            productionJobId = "job-01",
            title = "Paper gsm lower than specified",
            description = "Tested paper is 120gsm instead of required 150gsm art card.",
            category = DefectCategory.PAPER_OR_MATERIAL,
            severity = DefectSeverity.CRITICAL,
            source = DefectSource.CHECKLIST_INSPECTION,
            affectedQuantity = 2500,
            affectedUnit = "sheets",
            qcId = "qc-linked-01",
            inspectionChecklistId = "chk-linked-01",
            checklistItemId = "item-gsm-01",
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val defect = (result as DomainResult.Success).data
        assertNotNull(defect.defectId)
        assertEquals("qc-linked-01", defect.qcId)
        assertEquals("chk-linked-01", defect.inspectionChecklistId)
        assertEquals("item-gsm-01", defect.checklistItemId)

        val defectsForQc = defectRepository.observeDefectsByQc("qc-linked-01").first()
        assertEquals(1, defectsForQc.size)
        assertEquals(defect.defectId, defectsForQc[0].defectId)
    }
}
