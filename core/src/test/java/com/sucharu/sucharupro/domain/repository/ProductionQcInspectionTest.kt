package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionQcDataSource
import com.sucharu.sucharupro.data.repository.ProductionQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for starting, conducting, and completing QC inspections (Module 06 Step 01).
 */
class ProductionQcInspectionTest {

    private lateinit var qcDataSource: FakeProductionQcDataSource
    private lateinit var qcRepository: ProductionQcRepository

    private val sampleQc = ProductionQc(
        qcId = "qc-insp-01",
        productionJobId = "job-01",
        qcType = QcType.PRE_PRODUCTION,
        status = QcStatus.PENDING_INSPECTION,
        assignedInspectorId = "insp-01",
        assignedInspectorName = "রফিক আহমেদ",
        createdAt = "2026-08-16T10:00:00Z",
        updatedAt = "2026-08-16T10:00:00Z"
    )

    @Before
    fun setUp() {
        qcDataSource = FakeProductionQcDataSource(initialQcList = listOf(sampleQc))
        qcRepository = ProductionQcRepositoryImpl(qcDataSource)
    }

    @Test
    fun startInspection_transitionsStatusToInInspection() = runBlocking {
        val startRes = qcRepository.startInspection(
            qcId = "qc-insp-01",
            inspectorId = "insp-01",
            inspectorName = "রফিক আহমেদ",
            notes = "প্লেট অ্যালাইনমেন্ট পরীক্ষা শুরু",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(startRes is DomainResult.Success)
        val started = (startRes as DomainResult.Success).data
        assertEquals(QcStatus.IN_INSPECTION, started.status)
        assertEquals("2026-08-16T10:30:00Z", started.startedAt)
    }

    @Test
    fun completeInspection_withPassDecision_transitionsStatusToPassed() = runBlocking {
        qcRepository.startInspection(
            qcId = "qc-insp-01",
            inspectorId = "insp-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val completeRes = qcRepository.completeInspection(
            qcId = "qc-insp-01",
            decision = QcDecision.PASS,
            notes = "সব মানদণ্ড উত্তীর্ণ",
            inspectorId = "insp-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(QcStatus.PASSED, completed.status)
        assertEquals(QcDecision.PASS, completed.decision)
        assertNotNull(completed.completedAt)
    }

    @Test
    fun completeInspection_withFailDecision_transitionsStatusToFailed() = runBlocking {
        qcRepository.startInspection(
            qcId = "qc-insp-01",
            inspectorId = "insp-01",
            timestamp = "2026-08-16T10:30:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val completeRes = qcRepository.completeInspection(
            qcId = "qc-insp-01",
            decision = QcDecision.FAIL,
            notes = "কালার রেজিস্টারিং ত্রুটি পাওয়া গেছে",
            inspectorId = "insp-01",
            timestamp = "2026-08-16T11:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(completeRes is DomainResult.Success)
        val completed = (completeRes as DomainResult.Success).data
        assertEquals(QcStatus.FAILED, completed.status)
        assertEquals(QcDecision.FAIL, completed.decision)
    }
}
