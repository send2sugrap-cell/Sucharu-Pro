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

class BusinessFinancialReconciliationSnapshotTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)

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
    fun testReconciliationExecutionGeneratesTamperEvidentSnapshot() = runBlocking {
        val createRes = service.createReconciliationRun(
            staff,
            CreateReconciliationRunCommand(periodId = "PER-2026-08", runNumber = "RUN-SNP-01")
        )
        val runId = (createRes as DomainResult.Success).data.id

        val execRes = service.executeReconciliationRun(staff, runId)
        assertTrue(execRes is DomainResult.Success)

        val snapshot = repository.findSnapshotByRunId(runId, tenantId, projectId)
        assertNotNull(snapshot)
        assertNotNull(snapshot?.checksum)
        assertTrue(snapshot!!.checksum.isNotBlank())
        assertTrue(snapshot.snapshotData.contains("checked="))
    }
}
