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

class BusinessFinancialReconciliationSecurityTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialReconciliationDataSource()
        repository = BusinessFinancialReconciliationRepositoryImpl(dataSource)
        service = BusinessFinancialReconciliationServiceImpl(
            repository = repository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testForbiddenRolesAreDeniedAccess() = runBlocking {
        val forbiddenRoles = listOf(
            UserRole.CUSTOMER,
            UserRole.VENDOR,
            UserRole.AFFILIATE,
            UserRole.GUEST
        )

        for (role in forbiddenRoles) {
            val principal = AuthenticatedPrincipal(userId = "USR-FORBIDDEN", username = "forbidden_user", role = role, projectId = projectId)
            val createRes = service.createReconciliationRun(
                principal,
                CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "RUN-$role")
            )
            assertTrue("Role $role should be forbidden", createRes is DomainResult.Error)

            val listRes = service.listReconciliationRuns(principal)
            assertTrue("Role $role should be forbidden to list runs", listRes is DomainResult.Error)
        }
    }
}
