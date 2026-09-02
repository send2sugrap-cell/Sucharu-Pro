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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository tests for Pre-Production QC methods (Module 06 Step 02).
 */
class PreProductionQcRepositoryTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-repo-test-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.DRAFT,
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun initializePreProductionItems_idempotentBehavior() = runBlocking {
        val firstInit = qcRepository.initializePreProductionItems("qc-repo-test-01", UserRole.MANAGER)
        assertTrue(firstInit is DomainResult.Success)
        assertEquals(11, (firstInit as DomainResult.Success).data.size)

        val secondInit = qcRepository.initializePreProductionItems("qc-repo-test-01", UserRole.MANAGER)
        assertTrue(secondInit is DomainResult.Success)
        assertEquals(11, (secondInit as DomainResult.Success).data.size)

        val count = qcRepository.observePreProductionItems("qc-repo-test-01").first().size
        assertEquals(11, count)
    }
}
