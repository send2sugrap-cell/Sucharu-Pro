package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.ReconciliationRunType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.CreateReconciliationRunCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessFinancialReconciliationIsolationTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

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
    fun testProjectIsolation() = runBlocking {
        val proj1Admin = AuthenticatedPrincipal(userId = "ADMIN-1", username = "admin1", role = UserRole.ADMIN, projectId = "PRJ-001")
        val proj2Admin = AuthenticatedPrincipal(userId = "ADMIN-2", username = "admin2", role = UserRole.ADMIN, projectId = "PRJ-002")

        // Create Run in PRJ-001
        val createRes = service.createReconciliationRun(
            proj1Admin,
            CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "RUN-PRJ-01")
        )
        assertTrue(createRes is DomainResult.Success)
        val run1 = (createRes as DomainResult.Success).data

        // PRJ-002 listing should not see PRJ-001 runs
        val list2 = service.listReconciliationRuns(proj2Admin)
        assertTrue(list2 is DomainResult.Success)
        assertEquals(0, (list2 as DomainResult.Success).data.size)

        // PRJ-002 accessing PRJ-001 run by ID should fail
        val getByOtherProject = service.getReconciliationRunById(proj2Admin, run1.id)
        assertTrue(getByOtherProject is DomainResult.Error)
    }
}
