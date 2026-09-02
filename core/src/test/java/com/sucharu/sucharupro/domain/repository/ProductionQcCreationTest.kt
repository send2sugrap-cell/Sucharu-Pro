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
 * Tests for Production QC Creation and Initial Validation (Module 06 Step 01).
 */
class ProductionQcCreationTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource()
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun createQc_validParameters_createsDraftQc() = runBlocking {
        val result = qcRepository.createQc(
            productionJobId = "job-01",
            productionStageId = "stage-01",
            qcType = QcType.PRE_PRODUCTION,
            notes = "কাগজ ও কালার প্লেট চেকিং",
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Success)
        val qc = (result as DomainResult.Success).data
        assertNotNull(qc.qcId)
        assertEquals("job-01", qc.productionJobId)
        assertEquals("stage-01", qc.productionStageId)
        assertEquals(QcType.PRE_PRODUCTION, qc.qcType)
        assertEquals(QcStatus.DRAFT, qc.status)
        assertEquals(QcDecision.PENDING, qc.decision)

        val list = qcRepository.observeQcList().first()
        assertEquals(1, list.size)
    }

    @Test
    fun createQc_blankJobId_fails() = runBlocking {
        val result = qcRepository.createQc(
            productionJobId = "",
            qcType = QcType.FINAL,
            createdBy = "admin-01",
            timestamp = "2026-08-16T10:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(result is DomainResult.Error)
        val error = result as DomainResult.Error
        assertTrue(error.message.contains("Job ID cannot be blank"))
    }
}
