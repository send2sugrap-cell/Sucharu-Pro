package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinalQcDataSource
import com.sucharu.sucharupro.data.repository.FinalQcRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Repository creation tests for [FinalQcRepositoryImpl] (Module 06 Step 07).
 */
class FinalQcCreationTest {

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
    fun createFinalQc_success() = runBlocking {
        val result = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 1000,
            quantityUnit = "sheets",
            notes = "Final inspection for commercial print run",
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(result is DomainResult.Success)
        val fqc = (result as DomainResult.Success).data
        assertEquals("proj-01", fqc.projectId)
        assertEquals("job-01", fqc.productionJobId)
        assertEquals(1000, fqc.totalQuantity)
        assertEquals(FinalQcStatus.PENDING, fqc.status)
        assertEquals(FinalQcDecision.PENDING, fqc.decision)

        val list = repository.observeFinalQcList().first()
        assertEquals(1, list.size)
    }

    @Test
    fun createFinalQc_duplicateActive_rejected() = runBlocking {
        repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 1000,
            createdBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        val duplicate = repository.createFinalQc(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 1000,
            createdBy = "insp-02",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )

        assertTrue(duplicate is DomainResult.Error)
        assertTrue((duplicate as DomainResult.Error).message.contains("An active Final QC record already exists"))
    }
}
