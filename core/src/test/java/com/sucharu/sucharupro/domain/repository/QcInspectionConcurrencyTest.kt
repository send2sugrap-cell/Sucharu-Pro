package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Concurrency tests for concurrent responses and duplicate completion attempts (Module 06 Step 03).
 */
class QcInspectionConcurrencyTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleItem = QcChecklistItem(
        itemId = "item-conc-01",
        checklistTemplateId = "tmpl-01",
        categoryId = "cat-01",
        title = "কাটিং মার্জিন সঠিক",
        isRequired = true,
        createdAt = "2026-08-16T10:00:00Z"
    )

    private val sampleChecklist = QcInspectionChecklist(
        inspectionChecklistId = "chk-conc-01",
        inspectionId = "insp-conc-01",
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
            initialItems = listOf(sampleItem),
            initialChecklists = listOf(sampleChecklist)
        )
        checklistRepository = QcChecklistRepositoryImpl(checklistDataSource)
    }

    @Test
    fun concurrentCompletionAttempts_onlyOneSucceeds() = runBlocking {
        checklistRepository.saveResponse(
            inspectionId = "insp-conc-01",
            checklistItemId = "item-conc-01",
            status = QcResponseStatus.PASS,
            respondedBy = "insp-01",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val deferreds = (1..5).map { i ->
            async {
                checklistRepository.completeInspectionChecklist(
                    inspectionChecklistId = "chk-conc-01",
                    decision = QcDecision.PASS,
                    completedBy = "insp-0$i",
                    timestamp = "2026-08-16T10:30:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(1, successCount)
    }
}
