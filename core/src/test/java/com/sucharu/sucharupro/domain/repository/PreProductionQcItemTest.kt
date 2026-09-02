package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for individual Pre-Production QC item updates and statuses (Module 06 Step 02).
 */
class PreProductionQcItemTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-item-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.IN_INSPECTION,
        assignedInspectorId = "insp-01",
        assignedInspectorName = "Inspector 1",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        runBlocking {
            qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
            qcRepository = ProductionQcRepositoryImpl(qcDataSource)
            qcRepository.initializePreProductionItems("qc-item-01", UserRole.QC_INSPECTOR)
        }
    }

    @Test
    fun updatePreProductionItem_updatesStatusAndPreservesInspectorNotes() = runBlocking {
        val items = qcRepository.observePreProductionItems("qc-item-01").first()
        val firstItem = items.first()

        val updateRes = qcRepository.updatePreProductionItem(
            itemId = firstItem.itemId,
            status = PreProductionItemStatus.PASS,
            notes = "আর্টওয়ার্ক সাইজ ও রেজোলিউশন সঠিক",
            checkedBy = "insp-01",
            checkedByName = "Inspector 1",
            timestamp = "2026-08-16T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(updateRes is DomainResult.Success)
        val updated = (updateRes as DomainResult.Success).data
        assertEquals(PreProductionItemStatus.PASS, updated.status)
        assertEquals("আর্টওয়ার্ক সাইজ ও রেজোলিউশন সঠিক", updated.notes)
        assertEquals("insp-01", updated.checkedBy)
    }
}
