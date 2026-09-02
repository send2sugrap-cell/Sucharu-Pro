package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Thread-safety and race condition concurrency tests for QC Rework (Module 06 Step 05).
 */
class ProductionReworkConcurrencyTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun concurrentDuplicateCreation_resultsInExactlyOneActiveRework() = runBlocking {
        val tasks = (1..10).map { index ->
            async(Dispatchers.Default) {
                repository.createRework(
                    projectId = "proj-01",
                    productionJobId = "job-01",
                    defectId = "def-concurrent-01",
                    reworkType = ReworkType.PRINT_CORRECTION,
                    reason = ReworkReason.DEFECT_CORRECTION,
                    affectedQuantity = 50,
                    quantityUnit = "pcs",
                    description = "Thread $index attempt",
                    requestedBy = "insp-$index",
                    timestamp = "2026-08-17T10:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = tasks.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)
    }

    @Test
    fun concurrentCompletionAttempts_resultsInExactlyOneCompletion() = runBlocking {
        val createRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            reworkType = ReworkType.PRINT_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "Concurrent completion test",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (createRes as DomainResult.Success).data.reworkId
        repository.approveRework(reworkId, "mgr-01", "Manager", null, "2026-08-17T10:10:00Z", UserRole.MANAGER)
        repository.assignRework(reworkId, "tech-01", "Rahim", "mgr-01", "Manager", null, "2026-08-17T10:20:00Z", UserRole.MANAGER)
        repository.startRework(reworkId, "tech-01", "Rahim", "2026-08-17T10:30:00Z", UserRole.QC_INSPECTOR)

        val tasks = (1..10).map { index ->
            async(Dispatchers.Default) {
                repository.completeRework(
                    reworkId = reworkId,
                    correctiveAction = "Thread $index completion",
                    actualReworkedQuantity = 50,
                    completedBy = "tech-$index",
                    timestamp = "2026-08-17T11:00:00Z",
                    callerRole = UserRole.QC_INSPECTOR
                )
            }
        }

        val results = tasks.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        val errorCount = results.count { it is DomainResult.Error }

        assertEquals(1, successCount)
        assertEquals(9, errorCount)
    }
}
