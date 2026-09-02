package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for repository queries and CRUD operations (Module 06 Step 01).
 */
class ProductionQcRepositoryTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc1 = ProductionQc(
        qcId = "qc-repo-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.DRAFT,
        assignedInspectorId = "insp-01",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    private val sampleQc2 = ProductionQc(
        qcId = "qc-repo-02",
        productionJobId = "job-02",
        qcType = QcType.FINAL,
        status = QcStatus.DRAFT,
        assignedInspectorId = "insp-02",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc1, sampleQc2))
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun getQcById_returnsCorrectRecord() = runBlocking {
        val found = qcRepository.observeQcById("qc-repo-01").first()
        assertNotNull(found)
        assertEquals("qc-repo-01", found?.qcId)

        val notFound = qcRepository.observeQcById("non-existent").first()
        assertNull(notFound)
    }

    @Test
    fun getQcForJob_returnsFilteredRecords() = runBlocking {
        val job1List = qcRepository.getQcForJob("job-01").first()
        assertEquals(1, job1List.size)
        assertEquals("qc-repo-01", job1List.first().qcId)
    }

    @Test
    fun cancelQc_cancelsRecordWithMandatoryReason() = runBlocking {
        val cancelRes = qcRepository.cancelQc(
            qcId = "qc-repo-01",
            reason = "অর্ডার পরিবর্তন হওয়ায় বাতিল",
            cancelledBy = "admin-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.ADMIN
        )

        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(QcStatus.CANCELLED, cancelled.status)
    }
}
