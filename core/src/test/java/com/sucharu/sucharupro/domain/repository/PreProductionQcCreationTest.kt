package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcDecision
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
 * Tests for Pre-Production QC Creation and Item Initialization (Module 06 Step 02).
 */
class PreProductionQcCreationTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource()
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun createPreProductionQc_andInitializeItems_createsCanonicalChecklist() = runBlocking {
        val qcRes = qcRepository.createQc(
            productionJobId = "job-pre-01",
            qcType = QcType.PRE_PRODUCTION,
            notes = "প্রাক-উৎপাদন স্পেক ও প্লেট চেকিং",
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(qcRes is DomainResult.Success)
        val qc = (qcRes as DomainResult.Success).data
        assertEquals(QcType.PRE_PRODUCTION, qc.qcType)

        val initRes = qcRepository.initializePreProductionItems(qc.qcId, UserRole.MANAGER)
        assertTrue(initRes is DomainResult.Success)
        val items = (initRes as DomainResult.Success).data
        assertEquals(11, items.size)

        val observedItems = qcRepository.observePreProductionItems(qc.qcId).first()
        assertEquals(11, observedItems.size)
    }
}
