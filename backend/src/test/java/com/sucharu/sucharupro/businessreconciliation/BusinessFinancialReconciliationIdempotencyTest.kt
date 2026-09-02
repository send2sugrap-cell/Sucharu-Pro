package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.CreateReconciliationRunCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessFinancialReconciliationIdempotencyTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = "PRJ-001")

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialReconciliationDataSource()
        repository = BusinessFinancialReconciliationRepositoryImpl(dataSource)
        service = BusinessFinancialReconciliationServiceImpl(
            repository = repository,
            defaultTenantId = "TENANT-001"
        )
    }

    @Test
    fun testCreateRunIdempotency() = runBlocking {
        val cmd = CreateReconciliationRunCommand(
            periodId = "PER-2026-08",
            runNumber = "RUN-IDEM-01",
            idempotencyKey = "IDEM-KEY-1001"
        )

        val res1 = service.createReconciliationRun(admin, cmd)
        assertTrue(res1 is DomainResult.Success)
        val run1 = (res1 as DomainResult.Success).data

        // Re-call with same idempotency key returns the exact same entity
        val res2 = service.createReconciliationRun(admin, cmd)
        assertTrue(res2 is DomainResult.Success)
        val run2 = (res2 as DomainResult.Success).data

        assertEquals(run1.id, run2.id)
        assertEquals(run1.runNumber, run2.runNumber)

        val allRuns = (service.listReconciliationRuns(admin) as DomainResult.Success).data
        assertEquals(1, allRuns.size)
    }
}
