package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistCategory
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItem
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcResponseStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for inspector responses to checklist items (Module 06 Step 03).
 */
class QcInspectionResponseTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleItem = QcChecklistItem(
        itemId = "item-resp-01",
        checklistTemplateId = "tmpl-01",
        categoryId = "cat-01",
        title = "কাটিং মার্জিন ৩মিমি সঠিক আছে কিনা",
        itemType = QcChecklistItemType.PASS_FAIL,
        isRequired = true,
        createdAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        checklistDataSource = FakeQcChecklistDataSource(initialItems = listOf(sampleItem))
        checklistRepository = QcChecklistRepositoryImpl(checklistDataSource)
    }

    @Test
    fun saveResponse_pass_recordsResponseSuccessfully() = runBlocking {
        val res = checklistRepository.saveResponse(
            inspectionId = "insp-01",
            checklistItemId = "item-resp-01",
            status = QcResponseStatus.PASS,
            respondedBy = "insp-01",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(res is DomainResult.Success)
        val response = (res as DomainResult.Success).data
        assertEquals(QcResponseStatus.PASS, response.status)
        assertEquals("insp-01", response.respondedBy)
    }

    @Test
    fun saveResponse_failWithoutRemarks_failsValidation() = runBlocking {
        val res = checklistRepository.saveResponse(
            inspectionId = "insp-01",
            checklistItemId = "item-resp-01",
            status = QcResponseStatus.FAIL,
            remarks = "", // Missing required failure remarks!
            respondedBy = "insp-01",
            timestamp = "2026-08-16T10:35:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Failure remarks are required"))
    }
}
