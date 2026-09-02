package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Concurrency tests for simultaneous item updates and submission in Pre-Production QC (Module 06 Step 02).
 */
class PreProductionQcConcurrencyTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-conc-02",
        productionJobId = "job-02",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.IN_INSPECTION,
        assignedInspectorId = "insp-01",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        runBlocking {
            qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
            qcRepository = ProductionQcRepositoryImpl(qcDataSource)
            qcRepository.initializePreProductionItems("qc-conc-02", UserRole.QC_INSPECTOR)
        }
    }

    @Test
    fun concurrentItemUpdates_executeSafelyWithoutDataLoss() = runBlocking {
        val items = qcRepository.observePreProductionItems("qc-conc-02").first()

        val deferreds = items.map { item ->
            async {
                qcRepository.updatePreProductionItem(
                    itemId = item.itemId,
                    status = PreProductionItemStatus.PASS,
                    checkedBy = "insp-01",
                    timestamp = "2026-08-16T10:15:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = deferreds.awaitAll()
        assertEquals(items.size, results.count { it is DomainResult.Success })

        val finalItems = qcRepository.observePreProductionItems("qc-conc-02").first()
        assertEquals(items.size, finalItems.count { it.status == PreProductionItemStatus.PASS })
    }
}
