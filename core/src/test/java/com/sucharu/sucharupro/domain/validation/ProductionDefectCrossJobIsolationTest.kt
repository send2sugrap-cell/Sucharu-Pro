package com.sucharu.sucharupro.domain.validation

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
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cross-job isolation security tests for [ProductionDefect] (Module 06 Step 04).
 */
class ProductionDefectCrossJobIsolationTest {

    private lateinit var defectDataSource: FakeProductionDefectDataSource
    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var repository: ProductionDefectRepositoryImpl

    private val qcJobA = ProductionQc(
        qcId = "qc-job-A",
        productionJobId = "job-A",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.FAILED,
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    private val checklistJobA = QcInspectionChecklist(
        inspectionChecklistId = "chk-job-A",
        inspectionId = "insp-A",
        checklistTemplateId = "tmpl-01",
        checklistTemplateVersion = 1,
        productionJobId = "job-A",
        productionQcId = "qc-job-A",
        status = QcChecklistStatus.IN_PROGRESS,
        createdAt = "2026-08-17T10:00:00Z"
    )

    @Before
    fun setUp() {
        defectDataSource = FakeProductionDefectDataSource()
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(qcJobA))
        checklistDataSource = FakeQcChecklistDataSource(initialChecklists = listOf(checklistJobA))
        repository = ProductionDefectRepositoryImpl(
            defectDataSource = defectDataSource,
            qcDataSource = qcDataSource,
            checklistDataSource = checklistDataSource
        )
    }

    @Test
    fun createDefect_forJobB_referencingQcOfJobA_isRejected() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-B", // Target is Job B
            title = "Cross-job tampering attempt",
            description = "Attempting to link QC of Job A to Job B",
            category = DefectCategory.CONTENT_ERROR,
            severity = DefectSeverity.CRITICAL,
            source = DefectSource.CHECKLIST_INSPECTION,
            affectedQuantity = 10,
            qcId = "qc-job-A", // Belongs to Job A!
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Cross-job reference violation"))
    }

    @Test
    fun createDefect_forJobB_referencingChecklistOfJobA_isRejected() = runBlocking {
        val result = repository.createDefect(
            productionJobId = "job-B", // Target is Job B
            title = "Cross-job tampering attempt on checklist",
            description = "Attempting to link checklist of Job A to Job B",
            category = DefectCategory.CONTENT_ERROR,
            severity = DefectSeverity.CRITICAL,
            source = DefectSource.CHECKLIST_INSPECTION,
            affectedQuantity = 10,
            inspectionChecklistId = "chk-job-A", // Belongs to Job A!
            detectedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Cross-job reference violation"))
    }
}
