package com.sucharu.sucharupro.data.api.client

import com.sucharu.sucharupro.data.api.model.ApiResult
import com.sucharu.sucharupro.data.auth.model.LoginRequestDto
import com.sucharu.sucharupro.data.api.server.BackendApiServer
import com.sucharu.sucharupro.data.api.server.BackendRateLimiter
import com.sucharu.sucharupro.data.api.server.BackendRouter
import com.sucharu.sucharupro.data.api.server.BackendSecurityContext
import com.sucharu.sucharupro.data.api.server.BackendUseCases
import com.sucharu.sucharupro.data.datasource.FakeOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeOrderJobHandoffDataSource
import com.sucharu.sucharupro.data.event.MockPostgresEventDatabase
import com.sucharu.sucharupro.data.persistence.postgres.DatabaseHealthChecker
import com.sucharu.sucharupro.data.persistence.postgres.PostgresConnectionProvider
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.repository.OrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.repository.OrderRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class OrderProductionApiTest {

    private lateinit var orderDataSource: FakeOrderDataSource
    private lateinit var orderRepository: OrderRepository
    private lateinit var apiClient: DirectBackendApiClient
    private val orderId = "ORD-API-001"

    @Before
    fun setUp() {
        runBlocking {
            orderDataSource = FakeOrderDataSource()
            orderRepository = OrderRepositoryImpl(orderDataSource)

            val testOrder = Order(
                orderId = orderId,
                orderNumber = "ORD-2026-API",
                customerId = "CUST-API",
                status = OrderStatusType.CONFIRMED,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-API-1",
                        description = "Custom Printed Catalog",
                        quantity = 2500,
                        unitPrice = Money(BigDecimal("25.50"))
                    )
                ),
                createdAt = "2026-09-06T12:00:00Z",
                updatedAt = "2026-09-06T12:00:00Z"
            )
            orderDataSource.insertOrder(testOrder)

            val mockDb = MockPostgresEventDatabase()
            val mockConnProvider = object : PostgresConnectionProvider {
                override suspend fun acquireConnection(): Connection {
                    val mockRs: ResultSet = Proxy.newProxyInstance(
                        ResultSet::class.java.classLoader,
                        arrayOf(ResultSet::class.java)
                    ) { _, method, _ ->
                        if (method.name == "next") true
                        else if (method.name == "getInt") 1
                        else null
                    } as ResultSet

                    val mockStmt: Statement = Proxy.newProxyInstance(
                        Statement::class.java.classLoader,
                        arrayOf(Statement::class.java)
                    ) { _, method, _ ->
                        if (method.name == "executeQuery") mockRs
                        else null
                    } as Statement

                    return Proxy.newProxyInstance(
                        Connection::class.java.classLoader,
                        arrayOf(Connection::class.java)
                    ) { _, method, _ ->
                        if (method.name == "createStatement") mockStmt
                        else null
                    } as Connection
                }

                override suspend fun releaseConnection(connection: Connection) {}
                override fun close() {}
            }
            val securityContext = BackendSecurityContext()
            com.sucharu.sucharupro.data.auth.TestSecurityFixtures.registerStandardTestTokens(securityContext)

            val executionRepo = com.sucharu.sucharupro.data.repository.productionexecution.ProductionExecutionRepositoryImpl(
                com.sucharu.sucharupro.data.datasource.productionexecution.FakeProductionExecutionDataSource()
            )
            val fakeExecService = com.sucharu.sucharupro.domain.service.productionexecution.ProductionExecutionServiceImpl(
                executionRepository = executionRepo,
                orderRepository = orderRepository,
                planningRepository = com.sucharu.sucharupro.data.repository.productionplanning.ProductionPlanningRepositoryImpl(
                    com.sucharu.sucharupro.data.datasource.productionplanning.FakeProductionPlanningDataSource()
                ),
                commitmentRepository = com.sucharu.sucharupro.data.repository.commercialcommitment.CommercialCommitmentRepositoryImpl(
                    com.sucharu.sucharupro.data.datasource.commercialcommitment.FakeCommercialCommitmentDataSource()
                ),
                quoteRepository = com.sucharu.sucharupro.data.repository.printingquote.PrintingQuoteRepositoryImpl(
                    com.sucharu.sucharupro.data.datasource.printingquote.FakePrintingQuoteDataSource()
                )
            )
            val factory = object : PostgresRepositoryFactory(mockDb, defaultTenantId = "TENANT-001") {
                override fun createOrderProductionIntegrationService(
                    tenantId: String
                ): com.sucharu.sucharupro.domain.service.production.OrderProductionIntegrationService {
                    return com.sucharu.sucharupro.domain.service.production.OrderProductionIntegrationServiceImpl(
                        productionExecutionService = fakeExecService,
                        orderRepository = orderRepository
                    )
                }
            }
            val server = BackendApiServer(
                connectionProvider = mockConnProvider,
                transactionManager = mockDb,
                repositoryFactory = factory,
                securityContext = securityContext
            )
            server.start()
            val tokenStorage = com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage()
            tokenStorage.saveToken("token-staff-admin")
            apiClient = DirectBackendApiClient(server, tokenStorage)
        }
    }

    @Test
    fun testCreateProductionJobFromOrderApiSuccess() = runBlocking {
        val result = apiClient.createProductionJobFromOrder(orderId)

        assertTrue(result is ApiResult.Success)
        val dto = (result as ApiResult.Success).data
        assertEquals(orderId, dto.orderId)
        assertEquals("ORD-2026-API", dto.orderNumber)
        assertNotNull(dto.executionJobId)
    }

    @Test
    fun testGetProductionJobByOrderApiSuccess() = runBlocking {
        apiClient.createProductionJobFromOrder(orderId)

        val result = apiClient.getProductionJobByOrder(orderId)

        assertTrue(result is ApiResult.Success)
        val list = (result as ApiResult.Success).data
        assertTrue(list.isNotEmpty())
        assertEquals(orderId, list.first().orderId)
    }
}
