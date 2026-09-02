package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionDefectDataSource
import com.sucharu.sucharupro.data.repository.ProductionDefectRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Concurrency stress tests for [ProductionDefect] state transitions and assignments (Module 06 Step 04).
 */
class ProductionDefectConcurrencyTest {

    private lateinit var dataSource: FakeProductionDefectDataSource
    private lateinit var repository: ProductionDefectRepository

    private val sampleDefect = ProductionDefect(
        defectId = "def-conc-01",
        productionJobId = "job-01",
        category = DefectCategory.CUTTING_ERROR,
        severity = DefectSeverity.MAJOR,
        source = DefectSource.PRODUCTION_STAGE,
        status = DefectStatus.RESOLVED,
        title = "Guillotine drift",
        description = "Cut shifted by 2mm.",
        affectedQuantity = 150,
        resolutionNotes = "Calibrated blade backgauge",
        resolvedBy = "insp-01",
        resolvedAt = "2026-08-17T10:00:00Z",
        detectedAt = "2026-08-17T09:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T09:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    @Before
    fun setUp() {
        dataSource = FakeProductionDefectDataSource(initialDefects = listOf(sampleDefect))
        repository = ProductionDefectRepositoryImpl(dataSource)
    }

    @Test
    fun concurrentClosureAttempts_onlyFirstSucceeds() = runBlocking {
        val deferreds = (1..5).map { i ->
            async {
                repository.closeDefect(
                    defectId = "def-conc-01",
                    closedBy = "admin-$i",
                    closedByName = "Admin $i",
                    timestamp = "2026-08-17T10:30:00Z",
                    callerRole = UserRole.ADMIN
                )
            }
        }

        val results = deferreds.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(1, successCount) // Exactly one succeeds; others rejected because defect is already in terminal CLOSED state

        val finalDefect = (repository.findDefectById("def-conc-01") as DomainResult.Success).data
        assertEquals(DefectStatus.CLOSED, finalDefect.status)
        assertTrue(finalDefect.isTerminal)
    }
}
