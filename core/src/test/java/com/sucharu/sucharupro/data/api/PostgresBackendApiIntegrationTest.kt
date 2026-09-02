package com.sucharu.sucharupro.data.api

import com.sucharu.sucharupro.data.api.client.DirectBackendApiClient
import com.sucharu.sucharupro.data.api.client.InMemoryAuthTokenStorage
import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerCreditProfile
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.order.Order
import com.sucharu.sucharupro.domain.model.order.OrderItem
import com.sucharu.sucharupro.domain.model.order.OrderStatusType
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import com.sucharu.sucharupro.domain.model.order.JobHandoffStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Comprehensive Backend API Boundary & Client-Server Integration Test Suite (INFRA-02 Step 04).
 */
class PostgresBackendApiIntegrationTest {

    private lateinit var mockProvider: MockApiConnectionProvider
    private lateinit var transactionManager: TransactionManager
    private lateinit var repositoryFactory: PostgresRepositoryFactory
    private lateinit var securityContext: BackendSecurityContext
    private lateinit var server: BackendApiServer
    private lateinit var tokenStorage: InMemoryAuthTokenStorage
    private lateinit var apiClient: DirectBackendApiClient

    @Before
    fun setUp() {
        runBlocking {
            mockProvider = MockApiConnectionProvider()
            transactionManager = DefaultPostgresTransactionManager(mockProvider)
            repositoryFactory = PostgresRepositoryFactory(transactionManager, defaultTenantId = "TENANT-001")
            securityContext = BackendSecurityContext()
            com.sucharu.sucharupro.data.auth.TestSecurityFixtures.registerStandardTestTokens(securityContext)
            server = BackendApiServer(
                connectionProvider = mockProvider,
                transactionManager = transactionManager,
                repositoryFactory = repositoryFactory,
                securityContext = securityContext
            )
            server.start()

            tokenStorage = InMemoryAuthTokenStorage()
            apiClient = DirectBackendApiClient(server, tokenStorage)

            // Seed customer CUST-100 in Tenant-001
            val custRepo = repositoryFactory.createCustomerRepository("TENANT-001")
            custRepo.addCustomer(
                Customer(
                    customerId = "CUST-100",
                    customerCode = "ACME-01",
                    displayName = "Acme Commercial Prints",
                    primaryPhone = "+8801711000000",
                    email = "acme@example.com",
                    creditProfile = CustomerCreditProfile(creditLimit = Money(BigDecimal("10000.00"))),
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-23T10:00:00Z",
                    updatedAt = "2026-08-23T10:00:00Z"
                )
            )
        }
    }

    // =========================================================================
    // 1. PUBLIC ENDPOINTS (NO AUTH REQUIRED)
    // =========================================================================

    @Test
    fun `Public Endpoints - access succeeds without authentication`() = runBlocking {
        tokenStorage.clearToken()

        val company = apiClient.getPublicCompanyInfo()
        assertTrue("Company info must succeed", company.isSuccess)
        assertEquals("Sucharu Printing & Packaging", company.getOrNull()?.companyName)

        val products = apiClient.getPublicProducts()
        assertTrue("Public products catalog must succeed", products.isSuccess)
        assertTrue(products.getOrNull()!!.isNotEmpty())
    }

    // =========================================================================
    // 2. PROTECTED ENDPOINTS WITHOUT AUTH (RETURNS 401)
    // =========================================================================

    @Test
    fun `Protected Endpoints - missing auth token returns 401 UNAUTHENTICATED`() = runBlocking {
        tokenStorage.clearToken()

        val res = apiClient.getMyProfile()
        assertTrue("Must be error", res.isError)
        val err = (res as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.UNAUTHENTICATED, err.errorCode)
    }

    // =========================================================================
    // 3. CUSTOMER DATA ACCESS & HORIZONTAL ISOLATION
    // =========================================================================

    @Test
    fun `Customer Isolation - Customer A can access own data`() = runBlocking {
        tokenStorage.saveToken("token-customer-100")

        val profile = apiClient.getCustomerProfile()
        assertTrue("Customer A must access own profile", profile.isSuccess)
        assertEquals("CUST-100", profile.getOrNull()?.customerId)
        assertEquals("Acme Commercial Prints", profile.getOrNull()?.name)
    }

    @Test
    fun `Customer Isolation - Customer A cannot access Customer B order`() = runBlocking {
        // Seed order belonging to Customer B (CUST-200)
        val orderRepo = repositoryFactory.createOrderRepository("TENANT-001")
        orderRepo.createOrder(
            Order(
                orderId = "ORD-BETA-99",
                orderNumber = "SO-BETA-99",
                customerId = "CUST-200",
                status = OrderStatusType.CONFIRMED,
                priority = OrderPriority.NORMAL,
                items = listOf(
                    OrderItem(
                        itemId = "ITEM-01",
                        description = "Beta Prints",
                        quantity = 100,
                        unitPrice = Money(BigDecimal("10.00"))
                    )
                ),
                discount = Money.ZERO,
                jobHandoffStatus = JobHandoffStatus.NOT_READY,
                notes = "Confidential beta order",
                createdAt = "2026-08-23T10:00:00Z",
                updatedAt = "2026-08-23T10:00:00Z"
            )
        )

        // Customer A attempts to access Customer B's order
        tokenStorage.saveToken("token-customer-100")
        val res = apiClient.getCustomerOrderDetail("ORD-BETA-99")

        assertTrue("Customer A must be denied access to Customer B order", res.isError)
        val err = (res as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.FORBIDDEN, err.errorCode)
        assertTrue(err.message.contains("Access denied"))
    }

    // =========================================================================
    // 4. AFFILIATE ISOLATION
    // =========================================================================

    @Test
    fun `Affiliate Isolation - Affiliate A can access own commission summary`() = runBlocking {
        tokenStorage.saveToken("token-affiliate-100")

        val commission = apiClient.getAffiliateCommission()
        assertTrue("Affiliate must access own commission", commission.isSuccess)
        assertEquals("AFF-100", commission.getOrNull()?.affiliateId)
        assertEquals(BigDecimal("15400.00"), commission.getOrNull()?.totalCommissionEarned)
    }

    // =========================================================================
    // 5. TENANT ISOLATION & ANTI-SPOOFING
    // =========================================================================

    @Test
    fun `Tenant Isolation - Tenant B user cannot access Tenant A records`() = runBlocking {
        tokenStorage.saveToken("token-tenant-b-customer")

        // Tenant B user querying customer profile
        val profile = apiClient.getCustomerProfile()
        // Tenant B repository does not contain CUST-100
        assertTrue("Tenant B must get NOT_FOUND for non-existent profile in Tenant B", profile.isError)
        val err = (profile as ApiResult.Error).errorResponse
        assertEquals(ErrorCode.NOT_FOUND, err.errorCode)
    }

    // =========================================================================
    // 6. MUTATION ATOMICITY & IDEMPOTENCY
    // =========================================================================

    @Test
    fun `Mutation Safety - Create order commits atomically and supports Idempotency-Key`() = runBlocking {
        tokenStorage.saveToken("token-customer-100")

        val createReq = CreateOrderRequestDto(
            items = listOf(
                OrderItemRequestDto("Offset Catalogues", 500, BigDecimal("25.00")),
                OrderItemRequestDto("Custom Cartons", 200, BigDecimal("12.50"))
            ),
            notes = "Urgent commercial print batch",
            idempotencyKey = "IDEMP-KEY-999"
        )

        // 1. Initial Order Creation
        val order1 = apiClient.createCustomerOrder(createReq, idempotencyKey = "IDEMP-KEY-999")
        assertTrue("Initial order creation must succeed", order1.isSuccess)
        val orderId1 = order1.getOrNull()?.orderId
        assertNotNull(orderId1)
        assertEquals(BigDecimal("15000.00"), order1.getOrNull()?.totalAmount)

        // 2. Duplicate Submission with same Idempotency-Key
        val order2 = apiClient.createCustomerOrder(createReq, idempotencyKey = "IDEMP-KEY-999")
        assertTrue("Duplicate order creation must return cached response", order2.isSuccess)
        val orderId2 = order2.getOrNull()?.orderId
        assertEquals("Duplicate request must return identical order ID", orderId1, orderId2)
    }

    // =========================================================================
    // 7. HEALTH PROBES & GRACEFUL SHUTDOWN
    // =========================================================================

    @Test
    fun `Health & Shutdown - verifies live and ready probes and graceful termination`() = runBlocking {
        val live = apiClient.checkHealthLive()
        assertTrue(live.isSuccess)
        assertEquals("UP", live.getOrNull()?.get("status"))

        val ready = apiClient.checkHealthReady()
        assertTrue(ready.isSuccess)
        assertTrue(ready.getOrNull()?.isReady == true)

        server.shutdownGracefully()
        assertFalse(server.isServerRunning())
    }

    // =========================================================================
    // 8. SECURITY & SQL INJECTION DEFENSE
    // =========================================================================

    @Test
    fun `Security - SQL injection strings in parameters are safely handled`() = runBlocking {
        tokenStorage.saveToken("token-customer-100")

        val maliciousOrderId = "ORD-001' OR '1'='1"
        val res = apiClient.getCustomerOrderDetail(maliciousOrderId)
        assertTrue("SQL injection payload in order ID must return NOT_FOUND without database error", res.isError)
        assertEquals(ErrorCode.NOT_FOUND, (res as ApiResult.Error).errorResponse.errorCode)
    }
}

/**
 * Mock in-memory connection provider for API integration tests.
 */
class MockApiConnectionProvider : PostgresConnectionProvider {

    private val customers = mutableMapOf<String, Customer>()
    private val orders = mutableMapOf<String, Order>()
    var currentSessionProjectId: String = ""
    var isClosed = false

    override suspend fun acquireConnection(): java.sql.Connection {
        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.Connection::class.java.classLoader,
            arrayOf(java.sql.Connection::class.java),
            MockConnectionInvocationHandler()
        ) as java.sql.Connection
    }

    override suspend fun releaseConnection(connection: java.sql.Connection) {
        currentSessionProjectId = ""
    }

    override fun getActiveConnectionCount(): Int = 0
    override fun getIdleConnectionCount(): Int = 1
    override fun getTotalAcquisitions(): Long = 1L
    override fun getAcquisitionFailureCount(): Long = 0L

    override suspend fun shutdownGracefully(drainTimeoutMs: Long) {
        isClosed = true
    }

    override fun close() {
        isClosed = true
    }

    private inner class MockConnectionInvocationHandler : java.lang.reflect.InvocationHandler {
        private var inTx = false

        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?): Any? {
            val name = method.name
            val methodArgs = args ?: emptyArray()

            return when (name) {
                "setAutoCommit" -> {
                    inTx = !(methodArgs[0] as Boolean)
                    null
                }
                "getAutoCommit" -> !inTx
                "commit", "rollback" -> {
                    inTx = false
                    null
                }
                "isClosed" -> isClosed
                "isValid" -> true
                "close" -> null
                "prepareStatement" -> {
                    val sql = methodArgs[0] as String
                    createMockPreparedStatement(sql)
                }
                else -> null
            }
        }
    }

    private fun createMockPreparedStatement(sql: String): java.sql.PreparedStatement {
        val params = mutableListOf<Any?>()

        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.PreparedStatement::class.java.classLoader,
            arrayOf(java.sql.PreparedStatement::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "setString", "setInt", "setLong", "setBigDecimal" -> {
                        val idx = mArgs[0] as Int
                        val v = mArgs[1]
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "execute" -> {
                        if (sql.contains("set_config")) {
                            currentSessionProjectId = params.getOrNull(0) as? String ?: ""
                        }
                        true
                    }
                    "executeQuery" -> {
                        createMockResultSet(sql, params)
                    }
                    "executeUpdate" -> {
                        if (sql.contains("INSERT INTO customers")) {
                            val custId = params.getOrNull(1) as? String ?: ""
                            val code = params.getOrNull(2) as? String ?: ""
                            val name = params.getOrNull(3) as? String ?: ""
                            val type = params.getOrNull(4) as? String ?: "INDIVIDUAL"
                            val status = params.getOrNull(5) as? String ?: "ACTIVE"
                            val phone = params.getOrNull(6) as? String ?: "+8801700000000"
                            val altPhone = params.getOrNull(7) as? String
                            val email = params.getOrNull(8) as? String
                            val contactPerson = params.getOrNull(9) as? String
                            val limit = params.getOrNull(10) as? BigDecimal ?: BigDecimal.ZERO
                            val c = Customer(
                                customerId = custId,
                                customerCode = code,
                                displayName = name,
                                primaryPhone = phone,
                                alternatePhone = altPhone,
                                email = email,
                                contactPersonName = contactPerson,
                                creditProfile = CustomerCreditProfile(creditLimit = Money(limit)),
                                status = CustomerStatusType.valueOf(status),
                                createdAt = "2026-08-23T10:00:00Z",
                                updatedAt = "2026-08-23T10:00:00Z"
                            )
                            customers[custId] = c
                        } else if (sql.contains("INSERT INTO orders")) {
                            val orderId = params.getOrNull(1) as? String ?: ""
                            val orderNum = params.getOrNull(2) as? String ?: ""
                            val custId = params.getOrNull(3) as? String ?: ""
                            val status = params.getOrNull(5) as? String ?: "CONFIRMED"
                            val total = params.getOrNull(9) as? BigDecimal ?: BigDecimal.ZERO
                            val o = Order(
                                orderId = orderId,
                                orderNumber = orderNum,
                                customerId = custId,
                                quotationId = null,
                                approvedQuotationRevisionId = null,
                                status = OrderStatusType.valueOf(status),
                                priority = OrderPriority.NORMAL,
                                items = listOf(
                                    OrderItem("I1", "Prints", quantity = 100, unitPrice = Money(total))
                                ),
                                discount = Money.ZERO,
                                jobHandoffStatus = JobHandoffStatus.NOT_READY,
                                notes = "Order",
                                confirmedBy = "Admin",
                                createdAt = "2026-08-23T10:00:00Z",
                                updatedAt = "2026-08-23T10:00:00Z"
                            )
                            orders[orderId] = o
                        }
                        1
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.PreparedStatement
    }

    private fun createMockResultSet(sql: String, params: List<Any?>): java.sql.ResultSet {
        val rows = mutableListOf<Map<String, Any?>>()

        if (sql.contains("SELECT current_database()")) {
            rows.add(mapOf("1" to "sucharu_pro_db"))
        } else if (sql.contains("FROM customers") && sql.contains("customer_id = ?")) {
            val custId = params.getOrNull(1) as? String
            val c = customers[custId]
            if (c != null && (currentSessionProjectId == "TENANT-001" || currentSessionProjectId.isEmpty())) {
                rows.add(
                    mapOf(
                        "customer_id" to c.customerId,
                        "customer_code" to c.customerCode,
                        "display_name" to c.displayName,
                        "customer_type" to c.customerType.name,
                        "status" to c.status.name,
                        "primary_phone" to c.primaryPhone,
                        "alternate_phone" to c.alternatePhone,
                        "email" to c.email,
                        "contact_person_name" to c.contactPersonName,
                        "credit_limit_amount" to c.creditProfile.creditLimit.amount,
                        "credit_days" to c.creditProfile.paymentTermDays,
                        "notes" to c.notes,
                        "created_at" to java.sql.Timestamp(1755940000000L),
                        "updated_at" to java.sql.Timestamp(1755940000000L)
                    )
                )
            }
        } else if (sql.contains("FROM orders") && sql.contains("order_id = ?")) {
            val orderId = params.getOrNull(1) as? String
            val o = orders[orderId]
            if (o != null) {
                rows.add(
                    mapOf(
                        "order_id" to o.orderId,
                        "order_number" to o.orderNumber,
                        "customer_id" to o.customerId,
                        "quotation_id" to o.quotationId,
                        "status" to o.status.name,
                        "priority" to o.priority.name,
                        "discount_amount" to o.discount.amount,
                        "total_amount" to o.totalAmount.amount,
                        "job_handoff_status" to o.jobHandoffStatus.name,
                        "notes" to o.notes,
                        "confirmed_by" to o.confirmedBy,
                        "confirmed_at" to java.sql.Timestamp(1755940000000L),
                        "created_at" to java.sql.Timestamp(1755940000000L),
                        "updated_at" to java.sql.Timestamp(1755940000000L)
                    )
                )
            }
        } else if (sql.contains("FROM orders") && sql.contains("customer_id = ?")) {
            val custId = params.getOrNull(1) as? String
            for (o in orders.values) {
                if (o.customerId == custId) {
                    rows.add(
                        mapOf(
                            "order_id" to o.orderId,
                            "order_number" to o.orderNumber,
                            "customer_id" to o.customerId,
                            "quotation_id" to o.quotationId,
                            "status" to o.status.name,
                            "priority" to o.priority.name,
                            "discount_amount" to o.discount.amount,
                            "total_amount" to o.totalAmount.amount,
                            "job_handoff_status" to o.jobHandoffStatus.name,
                            "notes" to o.notes,
                            "confirmed_by" to o.confirmedBy,
                            "confirmed_at" to java.sql.Timestamp(1755940000000L),
                            "created_at" to java.sql.Timestamp(1755940000000L),
                            "updated_at" to java.sql.Timestamp(1755940000000L)
                        )
                    )
                }
            }
        }

        var idx = -1

        return java.lang.reflect.Proxy.newProxyInstance(
            java.sql.ResultSet::class.java.classLoader,
            arrayOf(java.sql.ResultSet::class.java),
            java.lang.reflect.InvocationHandler { _, method, args ->
                val mArgs = args ?: emptyArray()
                when (method.name) {
                    "next" -> {
                        idx++
                        idx < rows.size
                    }
                    "getString" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) rows[idx][col] as? String
                        else if (colIdx != null && colIdx == 1) rows[idx].values.firstOrNull() as? String
                        else null
                    }
                    "getBigDecimal" -> {
                        val col = mArgs[0] as String
                        rows[idx][col] as? BigDecimal ?: BigDecimal.ZERO
                    }
                    "getInt" -> {
                        val col = mArgs[0] as String
                        (rows[idx][col] as? Number)?.toInt() ?: 0
                    }
                    "getLong" -> {
                        val col = mArgs[0] as? String
                        val colIdx = mArgs[0] as? Int
                        if (col != null) (rows[idx][col] as? Number)?.toLong() ?: 0L
                        else if (colIdx != null && colIdx == 1) (rows[idx].values.firstOrNull() as? Number)?.toLong() ?: 0L
                        else 0L
                    }
                    "getTimestamp" -> {
                        val col = mArgs[0] as String
                        rows[idx][col] as? java.sql.Timestamp
                    }
                    "close" -> null
                    else -> null
                }
            }
        ) as java.sql.ResultSet
    }
}
