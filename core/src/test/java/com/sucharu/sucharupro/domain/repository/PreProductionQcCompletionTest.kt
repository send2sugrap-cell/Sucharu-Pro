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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for Pre-Production QC submission and overall completion outcome (Module 06 Step 02).
 */
class PreProductionQcCompletionTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-comp-01",
        productionJobId = "job-01",
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
            qcRepository.initializePreProductionItems("qc-comp-01", UserRole.QC_INSPECTOR)
        }
    }

    @Test
    fun submitQc_whenAllPass_transitionsToPassed() = runBlocking {
        val items = qcRepository.observePreProductionItems("qc-comp-01").first()
        items.forEach { item ->
            qcRepository.updatePreProductionItem(
                itemId = item.itemId,
                status = PreProductionItemStatus.PASS,
                checkedBy = "insp-01",
                timestamp = "2026-08-16T10:15:00Z",
                callerRole = UserRole.QC_INSPECTOR
            )
        }

        val submitRes = qcRepository.submitPreProductionQc(
            qcId = "qc-comp-01",
            decision = QcDecision.PASS,
            submittedBy = "insp-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(submitRes is DomainResult.Success)
        val qc = (submitRes as DomainResult.Success).data
        assertEquals(QcStatus.PASSED, qc.status)
        assertEquals(QcDecision.PASS, qc.decision)
    }

    @Test
    fun submitQc_whenItemFails_transitionsToFailed() = runBlocking {
        val items = qcRepository.observePreProductionItems("qc-comp-01").first()
        items.forEachIndexed { index, item ->
            val status = if (index == 0) PreProductionItemStatus.FAIL else PreProductionItemStatus.PASS
            qcRepository.updatePreProductionItem(
                itemId = item.itemId,
                status = status,
                checkedBy = "insp-01",
                timestamp = "2026-08-16T10:15:00Z",
                callerRole = UserRole.QC_INSPECTOR
            )
        }

        val submitRes = qcRepository.submitPreProductionQc(
            qcId = "qc-comp-01",
            decision = QcDecision.FAIL,
            submittedBy = "insp-01",
            notes = "কাগজের জিএসএম অমিল",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(submitRes is DomainResult.Success)
        val qc = (submitRes as DomainResult.Success).data
        assertEquals(QcStatus.FAILED, qc.status)
        assertEquals(QcDecision.FAIL, qc.decision)
    }
}
