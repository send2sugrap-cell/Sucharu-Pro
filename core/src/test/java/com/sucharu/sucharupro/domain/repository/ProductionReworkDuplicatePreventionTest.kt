package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeProductionReworkDataSource
import com.sucharu.sucharupro.data.repository.ProductionReworkRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests verifying duplicate active rework prevention for the same defect (Module 06 Step 05).
 */
class ProductionReworkDuplicatePreventionTest {

    private lateinit var dataSource: FakeProductionReworkDataSource
    private lateinit var repository: ProductionReworkRepository

    @Before
    fun setUp() {
        dataSource = FakeProductionReworkDataSource()
        repository = ProductionReworkRepositoryImpl(dataSource)
    }

    @Test
    fun creatingDuplicateActiveRework_forSameDefect_fails() = runBlocking {
        // First rework request for defect def-100
        val firstRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            defectId = "def-100",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            quantityUnit = "sheets",
            description = "1st Rework Request",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(firstRes is DomainResult.Success)

        // Attempt second active rework request for exact same defect
        val secondRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            defectId = "def-100",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            quantityUnit = "sheets",
            description = "2nd Duplicate Rework Request",
            requestedBy = "insp-02",
            timestamp = "2026-08-17T10:05:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(secondRes is DomainResult.Error)
        val error = secondRes as DomainResult.Error
        assertTrue(error.message.contains("An active rework request already exists for defect 'def-100'"))
    }

    @Test
    fun creatingNewRework_afterPreviousReworkCancelled_succeeds() = runBlocking {
        val firstRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            defectId = "def-100",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            quantityUnit = "sheets",
            description = "1st Rework Request",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        val reworkId = (firstRes as DomainResult.Success).data.reworkId

        // Cancel first rework
        repository.cancelRework(reworkId, "Cancelled by supervisor", "admin-01", "Admin", "2026-08-17T10:10:00Z", UserRole.ADMIN)

        // Now creating a new rework for defect def-100 should succeed since previous is terminal
        val secondRes = repository.createRework(
            projectId = "proj-01",
            productionJobId = "job-01",
            defectId = "def-100",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            affectedQuantity = 50,
            quantityUnit = "sheets",
            description = "Replacement Rework Request",
            requestedBy = "insp-02",
            timestamp = "2026-08-17T10:15:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(secondRes is DomainResult.Success)
    }
}
