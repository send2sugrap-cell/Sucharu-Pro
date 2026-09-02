package com.sucharu.sucharupro.domain.service.imposition

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.imposition.CalculateImpositionRequestDto
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.imposition.FakeImpositionDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.imposition.ImpositionRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class ImpositionSecurityEdgeTest {

    private lateinit var useCases: BackendUseCases
    private val tenantAlpha = "TENANT-AAA"
    private val tenantBeta = "TENANT-BBB"

    @Before
    fun setup() {
        val fakeImpositionDs = FakeImpositionDataSource()
        val impositionRepo = ImpositionRepositoryImpl(fakeImpositionDs)
        val impositionService = ImpositionServiceImpl(impositionRepo)

        val mockDb = MockPostgresEventDatabase()

        val factory = object : PostgresRepositoryFactory(
            transactionManager = mockDb,
            defaultTenantId = tenantAlpha
        ) {
            override fun createImpositionDataSource(tenantId: String) = fakeImpositionDs
            override fun createImpositionRepository(tenantId: String) = impositionRepo
            override fun createImpositionService(tenantId: String) = impositionService
        }

        useCases = BackendUseCases(mockDb, factory)
    }

    @Test
    fun testTenantIsolation_CrossTenantAccessReturnsNullOrEmpty() = runBlocking {
        val principalTenantA = AuthenticatedPrincipal(
            userId = "USER-A",
            username = "alice",
            email = "alice@tenant-a.com",
            role = UserRole.MANAGER,
            projectId = tenantAlpha
        )

        val principalTenantB = AuthenticatedPrincipal(
            userId = "USER-B",
            username = "bob",
            email = "bob@tenant-b.com",
            role = UserRole.MANAGER,
            projectId = tenantBeta
        )

        val req = CalculateImpositionRequestDto(
            jobId = "JOB-TENANT-A",
            orderId = "ORD-A",
            orderItemId = "ITEM-A",
            productName = "Tenant A Brochure",
            finishedItemWidth = BigDecimal("210.0000"),
            finishedItemHeight = BigDecimal("297.0000"),
            parentSheetWidth = BigDecimal("635.0000"),
            parentSheetHeight = BigDecimal("914.4000"),
            requiredQuantity = 1000L,
            saveSpecification = true
        )

        val createdA = useCases.calculateImpositionLayout(principalTenantA, req)
        assertNotNull(createdA.impositionId)
        assertEquals(tenantAlpha, createdA.tenantId)

        // Tenant B attempts to fetch Tenant A's imposition specification
        val resultTenantB = useCases.getImpositionSpecification(principalTenantB, createdA.impositionId)
        assertNull("Tenant B must NOT access Tenant A's imposition specification", resultTenantB)

        // Tenant B lists all - must not see Tenant A's record
        val listTenantB = useCases.listAllImpositions(principalTenantB)
        assertFalse("Tenant B list must not contain Tenant A's imposition", listTenantB.any { it.impositionId == createdA.impositionId })
    }

    @Test
    fun testUnauthorizedRole_CustomerRejectedFromImpositionCalculation() {
        val customerPrincipal = AuthenticatedPrincipal(
            userId = "USER-CUST",
            username = "customer",
            email = "customer@client.com",
            role = UserRole.CUSTOMER,
            projectId = tenantAlpha
        )

        val req = CalculateImpositionRequestDto(
            orderId = "ORD-CUST",
            orderItemId = "ITEM-CUST",
            productName = "Unauthorized Layout Attempt",
            finishedItemWidth = BigDecimal("210.0000"),
            finishedItemHeight = BigDecimal("297.0000"),
            parentSheetWidth = BigDecimal("635.0000"),
            parentSheetHeight = BigDecimal("914.4000"),
            requiredQuantity = 500L
        )

        assertThrows(ForbiddenException::class.java) {
            runBlocking {
                useCases.calculateImpositionLayout(customerPrincipal, req)
            }
        }
    }
}
