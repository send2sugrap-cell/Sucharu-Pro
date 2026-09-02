package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
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
 * Tests for QC Checklist Template Creation and Field Validation (Module 06 Step 03).
 */
class QcChecklistTemplateCreationTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    @Before
    fun setUp() {
        checklistDataSource = FakeQcChecklistDataSource()
        checklistRepository = QcChecklistRepositoryImpl(checklistDataSource)
    }

    @Test
    fun createTemplate_validParameters_createsActiveTemplateV1() = runBlocking {
        val result = checklistRepository.createTemplate(
            name = "অফসেট প্রিন্ট কোয়ালিটি চেকলিস্ট",
            description = "প্রিন্ট ও কালার নির্ভুলতা যাচাইয়ের টেমপ্লেট",
            qcType = QcType.FINAL,
            applicableStageType = "PRINTING",
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val template = (result as DomainResult.Success).data
        assertNotNull(template.checklistTemplateId)
        assertEquals("অফসেট প্রিন্ট কোয়ালিটি চেকলিস্ট", template.name)
        assertEquals(1, template.version)
        assertTrue(template.isActive)

        val templates = checklistRepository.observeTemplates().first()
        assertEquals(1, templates.size)
    }

    @Test
    fun createTemplate_blankName_failsValidation() = runBlocking {
        val result = checklistRepository.createTemplate(
            name = "",
            qcType = QcType.FINAL,
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("name cannot be blank"))
    }
}
