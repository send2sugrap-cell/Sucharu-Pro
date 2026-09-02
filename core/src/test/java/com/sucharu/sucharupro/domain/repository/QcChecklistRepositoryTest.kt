package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for repository queries and template lifecycle operations (Module 06 Step 03).
 */
class QcChecklistRepositoryTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleTemplate = QcChecklistTemplate(
        checklistTemplateId = "tmpl-repo-01",
        name = "কালার কিউসি টেমপ্লেট",
        qcType = QcType.FINAL,
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
    fun getTemplateById_returnsCorrectTemplate() = runBlocking {
        val found = checklistRepository.getTemplateById("tmpl-repo-01").first()
        assertNotNull(found)
        assertEquals("tmpl-repo-01", found?.checklistTemplateId)
    }

    @Test
    fun deactivateTemplate_setsIsActiveToFalse() = runBlocking {
        val deactRes = checklistRepository.deactivateTemplate(
            templateId = "tmpl-repo-01",
            callerRole = UserRole.MANAGER
        )
        assertTrue(deactRes is DomainResult.Success)
        val updated = (deactRes as DomainResult.Success).data
        assertFalse(updated.isActive)
    }
}
