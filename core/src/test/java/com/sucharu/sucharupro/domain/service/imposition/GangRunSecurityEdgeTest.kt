package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.imposition.GangRunCandidateItemDto
import com.sucharu.sucharupro.data.api.model.imposition.OptimizeGangRunRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.imposition.FakeGangRunDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.imposition.GangRunRepositoryImpl
import com.sucharu.sucharupro.domain.repository.imposition.GangRunRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Security, RBAC & Multi-Tenant Isolation Tests for Gang-Run Batching.
 * Module 18 Step 02.
 */
class GangRunSecurityEdgeTest {

    private lateinit var fakeDataSource: FakeGangRunDataSource
    private lateinit var repository: GangRunRepository
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var useCases: BackendUseCases

    @Before
    fun setUp() {
        fakeDataSource = FakeGangRunDataSource()
        repository = GangRunRepositoryImpl(fakeDataSource)
        val mockDb = MockPostgresEventDatabase()

        repositoryFactory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = "default"
        ) {
            override fun createGangRunDataSource(tenantId: String): com.sucharu.sucharupro.data.datasource.imposition.GangRunDataSource {
                return fakeDataSource
            }
            override fun createGangRunRepository(tenantId: String): GangRunRepository {
                return repository
            }
            override fun createGangRunService(tenantId: String): GangRunService {
                return GangRunServiceImpl(repository)
            }
        }

        useCases = BackendUseCases(mockDb, repositoryFactory)
    }

    @Test
    fun `cross-tenant access should be isolated`() = runBlocking {
        val tenantAPrincipal = AuthenticatedPrincipal(
            userId = "user_a",
            username = "alice",
            email = "alice@tenant-a.com",
            projectId = "TENANT_A",
            role = UserRole.MANAGER
        )
        val tenantBPrincipal = AuthenticatedPrincipal(
            userId = "user_b",
            username = "bob",
            email = "bob@tenant-b.com",
            projectId = "TENANT_B",
            role = UserRole.MANAGER
        )

        val request = OptimizeGangRunRequestDto(
            batchName = "Tenant A Gang Form",
            candidates = listOf(
                GangRunCandidateItemDto(
                    jobId = "JOB-TENANT-A",
                    orderId = "ORD-A",
                    orderItemId = "ITEM-A",
                    productName = "Tenant A Product",
                    finishedWidthMm = BigDecimal("100.0000"),
                    finishedHeightMm = BigDecimal("150.0000"),
                    requiredQuantity = 1000L,
                    paperStockType = "ART_CARD",
                    gsm = BigDecimal("300.0000")
                )
            )
        )

        val specsA = useCases.optimizeGangRunBatch(tenantAPrincipal, request)
        assertEquals(1, specsA.size)
        val gangRunId = specsA.first().gangRunId

        // Tenant A can retrieve
        val specFound = useCases.getGangRunSpecification(tenantAPrincipal, gangRunId)
        assertNotNull(specFound)

        // Tenant B cannot retrieve Tenant A's gang-run
        val specNotFound = useCases.getGangRunSpecification(tenantBPrincipal, gangRunId)
        assertNull(specNotFound)
    }

    @Test
    fun `unauthorized roles should be rejected with ForbiddenException`() {
        val customerPrincipal = AuthenticatedPrincipal(
            userId = "cust_1",
            username = "cust",
            email = "cust@client.com",
            projectId = "TENANT_A",
            role = UserRole.CUSTOMER
        )
        val vendorPrincipal = AuthenticatedPrincipal(
            userId = "vend_1",
            username = "vendor",
            email = "vend@vendor.com",
            projectId = "TENANT_A",
            role = UserRole.VENDOR
        )

        val request = OptimizeGangRunRequestDto(
            batchName = "Unauthorized Test",
            candidates = listOf(
                GangRunCandidateItemDto(
                    jobId = "JOB-01",
                    orderId = "ORD-01",
                    orderItemId = "ITEM-01",
                    productName = "Item 01",
                    finishedWidthMm = BigDecimal("100.0000"),
                    finishedHeightMm = BigDecimal("150.0000"),
                    requiredQuantity = 500L,
                    paperStockType = "ART_CARD",
                    gsm = BigDecimal("300.0000")
                )
            )
        )

        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.optimizeGangRunBatch(customerPrincipal, request)
            }
        }

        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.optimizeGangRunBatch(vendorPrincipal, request)
            }
        }
    }
}
