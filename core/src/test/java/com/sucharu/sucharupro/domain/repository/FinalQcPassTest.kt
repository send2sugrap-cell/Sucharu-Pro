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
 * Inspection pass tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcPassTest {

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
    fun passInspection_success() = runBlocking {
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

        val passRes = repository.submitPass(
            finalQcId = finalQcId,
            acceptedQuantity = 500,
            notes = "All 500 copies verified against master proof.",
            inspectorId = "insp-01",
            inspectorName = "Tariq Inspector",
            timestamp = "2026-08-17T10:20:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(passRes is DomainResult.Success)
        val passed = (passRes as DomainResult.Success).data
        assertEquals(FinalQcStatus.PASSED, passed.status)
        assertEquals(FinalQcDecision.PASS, passed.decision)
        assertEquals(500, passed.acceptedQuantity)
        assertEquals(0, passed.rejectedQuantity)
        assertEquals("insp-01", passed.inspectedBy)
    }
}
