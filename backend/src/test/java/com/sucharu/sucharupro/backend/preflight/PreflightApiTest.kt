package com.sucharu.sucharupro.backend.preflight

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.preflight.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.preflight.FakePreflightDataSource
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.preflight.PreflightRepositoryImpl
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine
import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngineImpl
import com.sucharu.sucharupro.domain.preflight.PreflightOverallResult
import com.sucharu.sucharupro.domain.preflight.PreflightRuleRegistry
import com.sucharu.sucharupro.domain.preflight.PreflightRunStatus
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository
import com.sucharu.sucharupro.domain.service.preflight.PreflightService
import com.sucharu.sucharupro.domain.service.preflight.PreflightServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PreflightApiTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private val projectId = "TENANT-001"
    private val artworkId = "ARTWORK-E2E-01"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "staff_01",
        projectId = projectId,
        username = "staff_user",
        role = UserRole.STAFF
    )

    private val guestPrincipal = AuthenticatedPrincipal(
        userId = "guest_01",
        projectId = projectId,
        username = "guest_user",
        role = UserRole.GUEST
    )

    @Before
    fun setup() = runBlocking {
        val preflightDs = FakePreflightDataSource()
        val preflightRepo = PreflightRepositoryImpl(preflightDs)
        val ruleRegistry = PreflightRuleRegistry()
        val preflightEngine = PreflightEngineImpl(ruleRegistry, preflightRepo)
        val preflightService = PreflightServiceImpl(preflightEngine, preflightRepo)

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createPreflightRuleRegistry(): PreflightRuleRegistry = ruleRegistry
            override fun createPreflightRepository(tenantId: String): PreflightRepository = preflightRepo
            override fun createPreflightEngine(tenantId: String): PreflightEngine = preflightEngine
            override fun createPreflightService(tenantId: String): PreflightService = preflightService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)
        Unit
    }

    @Test
    fun test01_startPreflightRun_staff_success() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId,
            jobId = "JOB-API-01"
        )

        val runResp = useCases.startPreflightRun(staffPrincipal, req, customFactory)
        assertNotNull(runResp.preflightRunId)
        assertEquals(artworkId, runResp.artworkId)
        assertEquals(PreflightRunStatus.COMPLETED, runResp.status)

        // Get details
        val details = useCases.getPreflightRunDetails(staffPrincipal, runResp.preflightRunId, customFactory)
        assertNotNull(details)
        assertEquals(runResp.preflightRunId, details?.preflightRunId)
    }

    @Test
    fun test02_startPreflightRun_guestRole_forbidden() = runBlocking {
        val req = StartPreflightRequestDto(
            artworkId = artworkId
        )

        try {
            useCases.startPreflightRun(guestPrincipal, req, customFactory)
            fail("Expected ForbiddenException for Guest starting preflight run.")
        } catch (e: ForbiddenException) {
            assertTrue(e.message?.contains("Forbidden") == true || e.message?.contains("Access") == true)
        }
    }
}
