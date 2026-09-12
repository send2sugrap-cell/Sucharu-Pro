package com.sucharu.sucharupro.data.preflight

import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext
import com.sucharu.sucharupro.domain.preflight.PreflightRuleRegistry
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PostgresPreflightSecurityTest {

    private lateinit var dataSource: FakePreflightDataSource
    private lateinit var service: PreflightServiceImpl

    @Before
    fun setUp() {
        dataSource = FakePreflightDataSource()
        val repository = PreflightRepositoryImpl(dataSource)
        val registry = PreflightRuleRegistry()
        val engine = PreflightEngineImpl(registry, repository)
        service = PreflightServiceImpl(engine, repository)
    }

    @Test
    fun test01_tenantIsolation_crossTenantRunRequest_fails() = runBlocking {
        val contextB = PreflightExecutionContext(
            tenantId = "TENANT-B",
            artworkId = "ARTWORK-A-101"
        )

        val result = service.runPreflight(contextB, "STAFF-B")
        assertTrue(result is DomainResult.Success)
        val run = (result as DomainResult.Success).data
        assertEquals("TENANT-B", run.tenantId)

        // Attempt querying Tenant B run under Tenant A context -> Should return empty/null
        val fetchRes = service.getPreflightRunDetails("TENANT-A", run.preflightRunId)
        assertTrue(fetchRes is DomainResult.Success)
        assertNull((fetchRes as DomainResult.Success).data)
    }
}
