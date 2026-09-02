package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.ReconciliationRunStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.CreateReconciliationRunCommand
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessFinancialReconciliationConcurrencyTest {

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
    fun testConcurrentRunExecutionsSafelySequenced() = runBlocking {
        val createRes = service.createReconciliationRun(
            admin,
            CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "RUN-CONC-01")
        )
        assertTrue(createRes is DomainResult.Success)
        val runId = (createRes as DomainResult.Success).data.id

        val deferreds = (1..10).map {
            async {
                service.executeReconciliationRun(admin, runId)
            }
        }
        val results = deferreds.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val finalRun = (service.getReconciliationRunById(admin, runId) as DomainResult.Success).data
        assertEquals(ReconciliationRunStatus.COMPLETED, finalRun.status)
    }
}
