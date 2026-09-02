package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeQcChecklistDataSource
import com.sucharu.sucharupro.data.repository.QcChecklistRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcChecklistTemplate
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for deterministic QC Checklist Template version increments (Module 06 Step 03).
 */
class QcChecklistTemplateVersionTest {

    private lateinit var checklistDataSource: FakeQcChecklistDataSource
    private lateinit var checklistRepository: QcChecklistRepository

    private val sampleTemplate = QcChecklistTemplate(
        checklistTemplateId = "tmpl-ver-01",
        name = "ল্যামিনেশন ও কাটিং চেকলিস্ট",
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
    fun createTemplateVersion_incrementsVersionNumberDeterministically() = runBlocking {
        val updateRes = checklistRepository.createTemplateVersion(
            templateId = "tmpl-ver-01",
            createdBy = "mgr-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.MANAGER
        )

        assertTrue(updateRes is DomainResult.Success)
        val updated = (updateRes as DomainResult.Success).data
        assertEquals(2, updated.version)
        assertEquals("2026-08-16T11:00:00Z", updated.updatedAt)
    }
}
