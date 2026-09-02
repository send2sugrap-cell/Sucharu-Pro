package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Inspection fail tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcFailTest {

    private lateinit var finalQcDataSource: FakeFinalQcDataSource
    private lateinit var repository: FinalQcRepository

    @Before
    fun setUp() {
        runBlocking {
            finalQcDataSource = FakeFinalQcDataSource()
            repository = FinalQcRepositoryImpl(finalQcDataSource = finalQcDataSource)
        }
    }

    @Test
    fun failInspection_success() = runBlocking {
        val createRes = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val finalQcId = (createRes as DomainResult.Success).data.finalQcId

        repository.startInspection(
            finalQcId = finalQcId,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val failRes = repository.submitFail(
            finalQcId = finalQcId,
            rejectedQuantity = 50,
            failureReason = "Severe glue leakage in spine binding on 50 books.",
            notes = "Spine binding temperature was set too high.",
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(failRes is DomainResult.Success)
        val failed = (failRes as DomainResult.Success).data
        assertEquals(FinalQcStatus.FAILED, failed.status)
        assertEquals(FinalQcDecision.FAIL, failed.decision)
        assertEquals(450, failed.acceptedQuantity)
        assertEquals(50, failed.rejectedQuantity)
        assertEquals("Severe glue leakage in spine binding on 50 books.", failed.failureReason)
    }
}
