package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency stress tests for QC inspector assignment and completion (Module 06 Step 01).
 */
class ProductionQcConcurrencyTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-conc-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.IN_INSPECTION,
        assignedInspectorId = "insp-01",
        assignedInspectorName = "Inspector 1",
        createdAt = "2026-08-16T10:00:00Z",
        startedAt = "2026-08-16T10:15:00Z",
        updatedAt = "2026-08-16T10:15:00Z"
    )

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun concurrentCompletionAttempts_onlyFirstSucceedsAndPreventsStateCorruption() = runBlocking {
        val deferreds = (1..5).map { i ->
            async {
                qcRepository.completeInspection(
                    qcId = "qc-conc-01",
                    decision = QcDecision.PASS,
                    notes = "Finished by inspector $i",
                    inspectorId = "insp-01",
                    timestamp = "2026-08-16T11:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(1, successCount) // Exactly one completion succeeds; subsequent attempts on terminal state fail

        val finalState = (qcRepository.findQcById("qc-conc-01") as DomainResult.Success).data
        assertEquals(QcStatus.PASSED, finalState.status)
        assertEquals(QcDecision.PASS, finalState.decision)
    }
}
