package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcInspectionResponse
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for completing inspection checklists and verifying PASS/FAIL rules (Module 06 Step 03).
 */
class QcInspectionCompletionTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleItem1 = QcChecklistItem(
        itemId = "item-comp-01",
        checklistTemplateId = "tmpl-01",
        categoryId = "cat-01",
        title = "কালার রেজিস্টারিং সঠিক",
        isRequired = true,
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleChecklist = QcInspectionChecklist(
        inspectionChecklistId = "chk-comp-01",
        inspectionId = "insp-01",
        checklistTemplateId = "tmpl-01",
        checklistTemplateVersion = 1,
        productionJobId = "job-01",
        productionQcId = "qc-01",
        status = QcChecklistStatus.IN_PROGRESS,
        createdAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        checklistDataSource = FakeQcChecklistDataSource(
            initialItems = listOf(sampleItem1),
            initialChecklists = listOf(sampleChecklist)
        )
        checklistRepository = QcChecklistRepositoryImpl(checklistDataSource)
    }

    @Test
    fun completeChecklist_withAllRequiredItemsPassed_succeeds() = runBlocking {
        checklistRepository.saveResponse(
            inspectionId = "insp-01",
            checklistItemId = "item-comp-01",
            status = QcResponseStatus.PASS,
            respondedBy = "insp-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val completeRes = checklistRepository.completeInspectionChecklist(
            inspectionChecklistId = "chk-comp-01",
            decision = QcDecision.PASS,
            completedBy = "insp-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(QcChecklistStatus.COMPLETED, completed.status)
    }

    @Test
    fun completeChecklist_withoutResponses_failsValidation() = runBlocking {
        val completeRes = checklistRepository.completeInspectionChecklist(
            inspectionChecklistId = "chk-comp-01",
            decision = QcDecision.PASS,
            completedBy = "insp-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(completeRes is DomainResult.Error)
        val error = completeRes as DomainResult.Error
        assertTrue(error.message.contains("pending required item"))
    }
}
