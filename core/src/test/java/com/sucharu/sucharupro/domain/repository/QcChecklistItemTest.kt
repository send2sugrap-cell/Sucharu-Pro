package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistItemType
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for QC Checklist Item creation and ordering (Module 06 Step 03).
 */
class QcChecklistItemTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleTemplate = QcChecklistTemplate(
        checklistTemplateId = "tmpl-item-01",
        name = "কালার ম্যাচিং চেকলিস্ট",
        qcType = QcType.PRE_PRODUCTION,
        version = 1,
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
    fun addItem_createsItemWithCorrectTypeAndCategory() = runBlocking {
        val catRes = checklistRepository.addCategory(
            templateId = "tmpl-item-01",
            name = "কালার পর্যবেক্ষণ",
            sequence = 1,
            callerRole = UserRole.MANAGER
        )
        val categoryId = (catRes as DomainResult.Success).data.categoryId

        val itemRes = checklistRepository.addItem(
            templateId = "tmpl-item-01",
            categoryId = categoryId,
            title = "ম্যাজেন্টা কালার ডেনসিটি চেকিং",
            sequence = 1,
            code = "CLR-01",
            itemType = QcChecklistItemType.NUMERIC,
            isRequired = true,
            expectedValue = "1.45",
            tolerance = "±0.05",
            unit = "Density",
            instructions = "ডেনসিটোমিটার দিয়ে ৩টি পয়েন্টে মাপ নিন",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(itemRes is DomainResult.Success)
        val item = (itemRes as DomainResult.Success).data
        assertEquals("ম্যাজেন্টা কালার ডেনসিটি চেকিং", item.title)
        assertEquals(QcChecklistItemType.NUMERIC, item.itemType)
        assertTrue(item.isRequired)

        val items = checklistRepository.observeItems("tmpl-item-01").first()
        assertEquals(1, items.size)
    }
}
