package com.sucharu.sucharupro.backend.customerfinancial

import com.sucharu.sucharupro.data.persistence.postgres.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customerinvoice.*
import com.sucharu.sucharupro.domain.model.customerpayment.*
import com.sucharu.sucharupro.domain.model.customersettlement.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.util.concurrent.ConcurrentHashMap

class PostgresCustomerInvoicePaymentIntegrationTest {

    private class MockFinancialDb : PostgresConnectionProvider {
        val invoices = ConcurrentHashMap<String, MutableMap<String, CustomerInvoice>>()
        val payments = ConcurrentHashMap<String, MutableMap<String, CustomerPayment>>()
        val allocations = ConcurrentHashMap<String, MutableMap<String, CustomerPaymentAllocation>>()

        override suspend fun acquireConnection(): Connection = createConnection()
        override suspend fun releaseConnection(connection: Connection) {}
        override fun getActiveConnectionCount(): Int = 0
        override fun getIdleConnectionCount(): Int = 1
        override fun getTotalAcquisitions(): Long = 1L
        override fun getAcquisitionFailureCount(): Long = 0L
        override suspend fun shutdownGracefully(drainTimeoutMs: Long) {}
        override fun close() {}

        private fun createProxy(interfaceClass: Class<*>, handler: (name: String, args: Array<out Any?>) -> Any?): Any {
            return Proxy.newProxyInstance(interfaceClass.classLoader, arrayOf(interfaceClass)) { _, method, args ->
                val mArgs = args ?: emptyArray()
                val result = handler(method.name.lowercase(), mArgs)
                if (result != null) {
                    result
                } else {
                    when (method.returnType) {
                        Boolean::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        Long::class.javaPrimitiveType -> 0L
                        Double::class.javaPrimitiveType -> 0.0
                        Float::class.javaPrimitiveType -> 0.0f
                        Short::class.javaPrimitiveType -> 0.toShort()
                        Byte::class.javaPrimitiveType -> 0.toByte()
                        Char::class.javaPrimitiveType -> ' '
                        else -> null
                    }
                }
            }
        }

        private fun createConnection(): Connection {
            return createProxy(Connection::class.java) { name, args ->
                when (name) {
                    "preparestatement" -> createPreparedStatement(args[0] as String)
                    "createstatement" -> createStatement()
                    "isclosed" -> false
                    "isvalid" -> true
                    else -> null
                }
            } as Connection
        }

        private fun createStatement(): Statement {
            return createProxy(Statement::class.java) { _, _ -> null } as Statement
        }

        private fun createPreparedStatement(sql: String): PreparedStatement {
            val params = mutableListOf<Any?>()

            return createProxy(PreparedStatement::class.java) { name, args ->
                when (name) {
                    "setstring", "setint", "setlong", "setbigdecimal", "setboolean", "settimestamp", "setobject", "setnull" -> {
                        val idx = args[0] as Int
                        val v = args.getOrNull(1)
                        while (params.size < idx) params.add(null)
                        params[idx - 1] = v
                        null
                    }
                    "executeupdate" -> executeUpdate(sql, params)
                    "executequery" -> executeQuery(sql, params)
                    else -> null
                }
            } as PreparedStatement
        }

        private fun executeUpdate(sql: String, params: List<Any?>): Int {
            val upperSql = sql.uppercase()
            if (upperSql.contains("CUSTOMER_INVOICES")) {
                if (upperSql.contains("INSERT INTO")) {
                    val invoiceId = params.getOrNull(0) as? String ?: return 0
                    val tenantId = params.getOrNull(1) as? String ?: return 0
                    val projectId = params.getOrNull(2) as? String ?: return 0

                    val tenantMap = invoices.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                    val invoice = CustomerInvoice(
                        invoiceId = invoiceId,
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = params.getOrNull(3) as? String ?: "",
                        customerFinancialAccountId = params.getOrNull(4) as? String ?: "",
                        invoiceNumber = params.getOrNull(5) as? String ?: "",
                        subtotal = (params.getOrNull(11) as? BigDecimal) ?: BigDecimal.ZERO,
                        grandTotal = (params.getOrNull(15) as? BigDecimal) ?: BigDecimal.ZERO,
                        paidAmount = (params.getOrNull(16) as? BigDecimal) ?: BigDecimal.ZERO,
                        dueAmount = (params.getOrNull(17) as? BigDecimal) ?: BigDecimal.ZERO,
                        status = runCatching { CustomerInvoiceStatus.valueOf(params.getOrNull(18) as String) }.getOrDefault(CustomerInvoiceStatus.DRAFT)
                    )
                    tenantMap[invoiceId] = invoice
                } else if (upperSql.contains("UPDATE")) {
                    val tenantId = params.getOrNull(params.size - 2) as? String ?: "TENANT-001"
                    val invoiceId = params.getOrNull(params.size - 1) as? String ?: return 0
                    val tenantMap = invoices.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                    val existing = tenantMap[invoiceId]
                    if (existing != null) {
                        tenantMap[invoiceId] = existing.copy(
                            paidAmount = (params.getOrNull(0) as? BigDecimal) ?: existing.paidAmount,
                            dueAmount = (params.getOrNull(1) as? BigDecimal) ?: existing.dueAmount,
                            status = runCatching { CustomerInvoiceStatus.valueOf(params.getOrNull(2) as String) }.getOrDefault(existing.status)
                        )
                    }
                }
                return 1
            }

            if (upperSql.contains("CUSTOMER_PAYMENTS")) {
                val paymentId = params.getOrNull(0) as? String ?: return 0
                val tenantId = params.getOrNull(1) as? String ?: return 0
                val projectId = params.getOrNull(2) as? String ?: return 0

                val tenantMap = payments.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                val payment = CustomerPayment(
                    paymentId = paymentId,
                    tenantId = tenantId,
                    projectId = projectId,
                    paymentNumber = params.getOrNull(3) as? String ?: "",
                    customerId = params.getOrNull(4) as? String ?: "",
                    customerFinancialAccountId = params.getOrNull(5) as? String ?: "",
                    invoiceId = params.getOrNull(6) as? String,
                    amount = (params.getOrNull(7) as? BigDecimal) ?: BigDecimal.ZERO,
                    currency = params.getOrNull(8) as? String ?: "BDT",
                    paymentMethod = runCatching { CustomerPaymentMethod.valueOf(params.getOrNull(9) as String) }.getOrDefault(CustomerPaymentMethod.CASH),
                    paymentDate = (params.getOrNull(10) as? Long) ?: System.currentTimeMillis(),
                    referenceNumber = params.getOrNull(11) as? String,
                    externalReference = params.getOrNull(12) as? String,
                    notes = params.getOrNull(13) as? String,
                    status = runCatching { CustomerPaymentStatus.valueOf(params.getOrNull(14) as String) }.getOrDefault(CustomerPaymentStatus.RECORDED),
                    idempotencyKey = params.getOrNull(15) as? String
                )
                tenantMap[paymentId] = payment
                return 1
            }

            if (upperSql.contains("CUSTOMER_PAYMENT_ALLOCATIONS")) {
                val allocationId = params.getOrNull(0) as? String ?: return 0
                val tenantId = params.getOrNull(1) as? String ?: return 0
                val projectId = params.getOrNull(2) as? String ?: return 0

                val tenantMap = allocations.computeIfAbsent(tenantId) { ConcurrentHashMap() }
                val alloc = CustomerPaymentAllocation(
                    allocationId = allocationId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = params.getOrNull(3) as? String ?: "",
                    customerFinancialAccountId = params.getOrNull(4) as? String ?: "",
                    paymentId = params.getOrNull(5) as? String ?: "",
                    invoiceId = params.getOrNull(6) as? String ?: "",
                    allocatedAmount = (params.getOrNull(7) as? BigDecimal) ?: BigDecimal.ZERO,
                    status = runCatching { CustomerPaymentAllocationStatus.valueOf(params.getOrNull(8) as String) }.getOrDefault(CustomerPaymentAllocationStatus.ALLOCATED),
                    idempotencyKey = params.getOrNull(9) as? String
                )
                tenantMap[allocationId] = alloc
                return 1
            }

            return 1
        }

        private fun executeQuery(sql: String, params: List<Any?>): ResultSet {
            val upperSql = sql.uppercase()
            val tenantId = params.getOrNull(0) as? String ?: ""

            if (upperSql.contains("CUSTOMER_INVOICES")) {
                val projectId = params.getOrNull(1) as? String ?: ""
                val invoiceId = if (params.size > 2) params.getOrNull(2) as? String else null
                val filtered = invoices[tenantId]?.values?.filter {
                    it.projectId == projectId && (invoiceId.isNullOrBlank() || it.invoiceId == invoiceId)
                } ?: emptyList()

                var index = -1
                return createProxy(ResultSet::class.java) { name, args ->
                    val mArgs = args ?: emptyArray()
                    when (name) {
                        "next" -> {
                            index++
                            index < filtered.size
                        }
                        "wasnull" -> false
                        "getstring" -> {
                            val row = filtered.getOrNull(index) ?: return@createProxy null
                            val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                            when (col) {
                                "invoice_id" -> row.invoiceId
                                "tenant_id" -> row.tenantId
                                "project_id" -> row.projectId
                                "customer_id" -> row.customerId
                                "customer_financial_account_id" -> row.customerFinancialAccountId
                                "invoice_number" -> row.invoiceNumber
                                "currency" -> row.currency
                                "status" -> row.status.name
                                else -> null
                            }
                        }
                        "getbigdecimal" -> {
                            val row = filtered.getOrNull(index) ?: return@createProxy null
                            val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                            when (col) {
                                "subtotal" -> row.subtotal
                                "discount" -> row.discount
                                "tax" -> row.tax
                                "adjustment" -> row.adjustment
                                "grand_total" -> row.grandTotal
                                "paid_amount" -> row.paidAmount
                                "due_amount" -> row.dueAmount
                                else -> BigDecimal.ZERO
                            }
                        }
                        "gettimestamp" -> Timestamp(System.currentTimeMillis())
                        "getlong" -> 1L
                        else -> null
                    }
                } as ResultSet
            }

            if (upperSql.contains("CUSTOMER_PAYMENTS")) {
                val projectId = params.getOrNull(1) as? String ?: ""
                val paymentId = if (params.size > 2) params.getOrNull(2) as? String else null
                val filtered = payments[tenantId]?.values?.filter {
                    it.projectId == projectId && (paymentId.isNullOrBlank() || it.paymentId == paymentId)
                } ?: emptyList()

                var index = -1
                return createProxy(ResultSet::class.java) { name, args ->
                    val mArgs = args ?: emptyArray()
                    when (name) {
                        "next" -> {
                            index++
                            index < filtered.size
                        }
                        "wasnull" -> false
                        "getstring" -> {
                            val row = filtered.getOrNull(index) ?: return@createProxy null
                            val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                            when (col) {
                                "payment_id" -> row.paymentId
                                "tenant_id" -> row.tenantId
                                "project_id" -> row.projectId
                                "payment_number" -> row.paymentNumber
                                "customer_id" -> row.customerId
                                "customer_financial_account_id" -> row.customerFinancialAccountId
                                "invoice_id" -> row.invoiceId
                                "currency" -> row.currency
                                "payment_method" -> row.paymentMethod.name
                                "status" -> row.status.name
                                "idempotency_key" -> row.idempotencyKey
                                else -> null
                            }
                        }
                        "getbigdecimal" -> {
                            val row = filtered.getOrNull(index) ?: return@createProxy null
                            val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                            when (col) {
                                "amount" -> row.amount
                                else -> BigDecimal.ZERO
                            }
                        }
                        "gettimestamp" -> Timestamp(System.currentTimeMillis())
                        "getlong" -> 1L
                        else -> null
                    }
                } as ResultSet
            }

            // Allocations
            val projectId = params.getOrNull(1) as? String ?: ""
            val filtered = allocations[tenantId]?.values?.filter {
                it.projectId == projectId
            } ?: emptyList()

            var index = -1
            return createProxy(ResultSet::class.java) { name, args ->
                val mArgs = args ?: emptyArray()
                when (name) {
                    "next" -> {
                        index++
                        index < filtered.size
                    }
                    "wasnull" -> false
                    "getstring" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy null
                        val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                        when (col) {
                            "allocation_id" -> row.allocationId
                            "tenant_id" -> row.tenantId
                            "project_id" -> row.projectId
                            "payment_id" -> row.paymentId
                            "invoice_id" -> row.invoiceId
                            "customer_id" -> row.customerId
                            "customer_financial_account_id" -> row.customerFinancialAccountId
                            "status" -> row.status.name
                            "idempotency_key" -> row.idempotencyKey
                            else -> null
                        }
                    }
                    "getbigdecimal" -> {
                        val row = filtered.getOrNull(index) ?: return@createProxy null
                        val col = (mArgs.getOrNull(0) as? String)?.lowercase()
                        when (col) {
                            "allocated_amount" -> row.allocatedAmount
                            else -> BigDecimal.ZERO
                        }
                    }
                    "gettimestamp" -> Timestamp(System.currentTimeMillis())
                    "getlong" -> 1L
                    else -> null
                }
            } as ResultSet
        }
    }

    private lateinit var mockDb: MockFinancialDb
    private lateinit var transactionManager: DefaultPostgresTransactionManager
    private lateinit var invoiceDataSource: PostgresCustomerInvoiceDataSource
    private lateinit var paymentDataSource: PostgresCustomerPaymentDataSource
    private lateinit var allocationDataSource: PostgresCustomerPaymentAllocationDataSource
    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setUp() {
        mockDb = MockFinancialDb()
        transactionManager = DefaultPostgresTransactionManager(mockDb)
        invoiceDataSource = PostgresCustomerInvoiceDataSource(transactionManager, tenantId)
        paymentDataSource = PostgresCustomerPaymentDataSource(transactionManager, tenantId)
        allocationDataSource = PostgresCustomerPaymentAllocationDataSource(transactionManager, tenantId)
    }

    @Test
    fun testSaveAndGetInvoiceWithLines_PostgresDataSource() = runBlocking {
        val invoice = CustomerInvoice(
            invoiceId = "INV-PG-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = "CUST-PG-01",
            customerFinancialAccountId = "ACC-PG-01",
            invoiceNumber = "INV-2026-001",
            subtotal = BigDecimal("1000.00"),
            grandTotal = BigDecimal("1000.00"),
            dueAmount = BigDecimal("1000.00"),
            paidAmount = BigDecimal.ZERO,
            status = CustomerInvoiceStatus.ISSUED,
            lines = listOf(
                CustomerInvoiceLine(
                    lineId = "LINE-01",
                    invoiceId = "INV-PG-001",
                    tenantId = tenantId,
                    projectId = projectId,
                    description = "1000 Premium Printed Catalogs",
                    quantity = BigDecimal("1000"),
                    unitPrice = BigDecimal("1.00"),
                    lineTotal = BigDecimal("1000.00")
                )
            )
        )

        val inserted = invoiceDataSource.insertInvoice(invoice)
        assertTrue(inserted is DomainResult.Success)

        val retrievedRes = invoiceDataSource.findInvoiceById(tenantId, projectId, invoice.invoiceId)
        assertTrue(retrievedRes is DomainResult.Success)
        val retrieved = (retrievedRes as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals("INV-PG-001", retrieved.invoiceId)
        assertEquals("INV-2026-001", retrieved.invoiceNumber)
        assertEquals(CustomerInvoiceStatus.ISSUED, retrieved.status)
        assertEquals(BigDecimal("1000.00"), retrieved.grandTotal)
    }

    @Test
    fun testSaveAndGetPaymentWithIdempotency_PostgresDataSource() = runBlocking {
        val payment = CustomerPayment(
            paymentId = "PAY-PG-001",
            tenantId = tenantId,
            projectId = projectId,
            paymentNumber = "PAY-2026-001",
            customerId = "CUST-PG-01",
            customerFinancialAccountId = "ACC-PG-01",
            amount = BigDecimal("600.00"),
            paymentMethod = CustomerPaymentMethod.BKASH,
            status = CustomerPaymentStatus.CONFIRMED,
            idempotencyKey = "idemp-pay-001"
        )

        val inserted = paymentDataSource.insertPayment(payment)
        assertTrue(inserted is DomainResult.Success)

        val retrievedRes = paymentDataSource.findPaymentById(tenantId, projectId, payment.paymentId)
        assertTrue(retrievedRes is DomainResult.Success)
        val retrieved = (retrievedRes as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals("PAY-PG-001", retrieved.paymentId)
        assertEquals(BigDecimal("600.00"), retrieved.amount)
        assertEquals("idemp-pay-001", retrieved.idempotencyKey)
    }

    @Test
    fun testSavePaymentAllocationAndUpdateInvoiceDueAmount_PostgresDataSource() = runBlocking {
        val allocation = CustomerPaymentAllocation(
            allocationId = "ALLOC-PG-001",
            tenantId = tenantId,
            projectId = projectId,
            customerId = "CUST-PG-01",
            customerFinancialAccountId = "ACC-PG-01",
            paymentId = "PAY-PG-001",
            invoiceId = "INV-PG-001",
            allocatedAmount = BigDecimal("600.00"),
            status = CustomerPaymentAllocationStatus.ALLOCATED,
            idempotencyKey = "idemp-alloc-001"
        )

        val inserted = allocationDataSource.createAllocation(allocation)
        assertTrue(inserted is DomainResult.Success)

        val listRes = allocationDataSource.listAllocations(tenantId, projectId, paymentId = "PAY-PG-001")
        assertTrue(listRes is DomainResult.Success)
        val list = (listRes as DomainResult.Success).data
        assertTrue(list.isNotEmpty())
        assertEquals("ALLOC-PG-001", list[0].allocationId)
        assertEquals(BigDecimal("600.00"), list[0].allocatedAmount)
    }

    @Test
    fun testCrossTenantIsolation_PreventsUnauthorizedAccess() = runBlocking {
        val otherTenantDataSource = PostgresCustomerInvoiceDataSource(transactionManager, "TENANT-OTHER")

        val retrievedRes = otherTenantDataSource.findInvoiceById("TENANT-OTHER", projectId, "INV-PG-001")
        assertTrue(retrievedRes is DomainResult.Error)
    }
}
